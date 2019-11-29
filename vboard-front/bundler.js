/*
 * This file is part of the vboard distribution.
 * (https://github.com/voyages-sncf-technologies/vboard)
 * Copyright (c) 2017 VSCT.
 *
 * vboard is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, version 3.
 *
 * vboard is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU Affero
 * General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */

const buildify = require('buildify');
const crypto = require('crypto');
const flatten = (arrayOfArrays) => Array.prototype.concat.apply([], arrayOfArrays);
const fs = require('fs');
const glob = require('glob');
const http = require('http');
const moment = require('moment');
const nodemon = require('nodemon');
const path = require('path');

const SRC_JSON_FILE = './bundled.json';
const BUILD_DIR = './build';
const HTTP_PORT = 80;

exports.mkdirp = (dirPath) => {
    if (!fs.existsSync(dirPath)) {
        fs.mkdirSync(dirPath, 0o0766, (err) => {
            if (err) { console.error(err) }
        });
    }
};

exports.shortMd5 = (content) => {
    const hash = crypto.createHash('md5');
    hash.update(content);
    return hash.digest('hex').slice(0, 8);
}

exports.generateBundle = (srcFiles, dstName, ext, buildDir) => {
    let hash = null;
    buildify().concat(srcFiles)
        .perform((content) => { hash = this.shortMd5(content); return content; })
        .save(`${ buildDir  }/${ dstName }.${ hash }.${ ext }`);
    return hash;
};

exports.concatHtmlTemplates = () => glob.sync('src/main/common/*/templates/*.html').map((htmlFilepath) => `<script type="text/ng-template" id="${ htmlFilepath.replace('src/main/', '') }">${
    fs.readFileSync(htmlFilepath)
}</script>`).join('\n');

exports.copyIndexHtml = (buildDir, filerevs, htmlTemplates) => {
    buildify().load('src/main/index.html')
        .perform((content) => content.replace('${vendor.css.filename}', `vendor.${ filerevs.vendor.css }.css`)
            .replace('${app.css.filename}', `app.${ filerevs.app.css }.css`)
            .replace('${vendor.js.filename}', `vendor.${ filerevs.vendor.js }.js`)
            .replace('${app.js.filename}', `app.${ filerevs.app.js }.js`)
            .replace('${html_templates}', htmlTemplates))
        .save(`${ buildDir  }/index.html`);
};

exports.copyAdminVersion = (buildDir, version) => {
    buildify().load('src/main/admin-version.json')
        .perform((content) => content.replace('${version}', version)
            .replace('${buildDate}', moment().format('YYYYMMDD_HHmm')))
        .save(`${ buildDir  }/admin-version.json`);
};

exports.copyFiles = ({ srcFiles, dstDir }) => {
    flatten(srcFiles.map((srcFile) => glob.sync(srcFile))).forEach((srcFile) => {
        const dstFile = `${ dstDir  }/${  path.basename(srcFile) }`;
        console.log(dstFile);
        fs.copyFileSync(srcFile, dstFile);
    })
};

exports.build = (buildDir) => {
    this.mkdirp(buildDir);
    console.log('Generating app & vendor bundles:')
    const sourcesToBundle = JSON.parse(fs.readFileSync(SRC_JSON_FILE));
    const filerevs = {
        vendor: {
            js: this.generateBundle(sourcesToBundle.vendor.js, 'vendor', 'js', buildDir),
            css: this.generateBundle(sourcesToBundle.vendor.css, 'vendor', 'css', buildDir),
        },
        app: {
            js: this.generateBundle(glob.sync('src/main/**/*.js'), 'app', 'js', buildDir),
            css: this.generateBundle(['app-pleeeased.css'], 'app', 'css', buildDir),
        },
    };
    console.log('Filerevs:', JSON.stringify(filerevs));
    const htmlTemplates = this.concatHtmlTemplates();
    this.copyIndexHtml(buildDir, filerevs, htmlTemplates);
    const pkg = JSON.parse(fs.readFileSync('package.json'));
    this.copyAdminVersion(buildDir, pkg.version);
    this.copyFiles({ srcFiles: ['config.js', 'src/main/favicon.ico', 'src/main/deprecated_browser.html', 'node_modules/foundation-icon-fonts/foundation-icons.{eot,svg,ttf,woff}'],
        dstDir: buildDir });
    this.mkdirp(`${ buildDir  }/images`);
    this.copyFiles({ srcFiles: ['src/main/images/*{gif,png,jpeg,jpg,svg}'],
        dstDir: `${ buildDir  }/images` });
};

if (require.main === module) { // means we are executed as a script, not loaded as a lib
    exports.build(BUILD_DIR);
    if (process.argv[2] === '--watch-and-serve') {
        nodemon({ watch: ['src/main', 'config.js'] }).on('restart', () => exports.build(BUILD_DIR));
        // Starting a very basic server serving static files:
        http.createServer((request, response) => {
            let filePath = `${ BUILD_DIR  }/${  request.url === '/' ? 'index.html' : request.url }`;
            if (request.url.startsWith('/avatar')) {
                filePath = `../vboard-ws/imagesStorageDir/avatar${  request.url.substring(7) }`;
            } else if (request.url.startsWith('/pinImg')) {
                filePath = `../vboard-ws/imagesStorageDir/pinImg${  request.url.substring(7) }`;
            }
            console.log('Serving:', filePath);
            const contentType = {
                '.css': 'text/css',          '.js': 'text/javascript',
                '.jpg': 'image/jpg',         '.png': 'image/png',
                '.json': 'application/json',
            }[path.extname(filePath)] || 'text/html';
            fs.readFile(filePath, (error, content) => {
                if (error) {
                    response.writeHead(error.code === 'ENOENT' ? 404 : 500);
                    response.end();
                } else {
                    response.writeHead(200, { 'Content-Type': contentType });
                    response.end(content, 'utf-8');
                }
            });
        }).listen(HTTP_PORT);
    }
}

// IMHO, this build script is better than the previously used grunt-based build pipeline:
// - build logic is not split into very many config files & grunt plugins (there used to be > 20...)
// - there aren't several, hard-to-understand, layers of abstraction (Grunt targets system + aliases + usemin sub-pipeline + wiredep substitution tags + ...)
// - it is easily debuggable & hackable, one can easily introduce/test changes to the pipeline
// - it is lintable & unit-testable if need be
