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

package com.vsct.vboard.controllers;

import com.vsct.vboard.DAO.*;
import com.vsct.vboard.config.ProxyConfig;
import com.vsct.vboard.exceptions.DuplicateContentException;
import com.vsct.vboard.models.*;
import com.vsct.vboard.parameterFormat.AddNewPinParams;
import com.vsct.vboard.services.*;
import com.vsct.vboard.utils.JavaUtils;
import org.apache.commons.io.IOUtils;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.UnexpectedRollbackException;
import org.springframework.web.bind.annotation.*;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import javax.validation.Valid;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.apache.commons.lang3.StringUtils.isBlank;


@RestController
@RequestMapping(value = "/pins")
public class PinsController {
    private final Logger logger = LoggerFactory.getLogger(getClass());
    private final JdbcTemplate jdbcTemplate;
    private final PinDAO pinDAO;
    private final ElasticSearchClient elsClient;
    private final UploadsManager uploadsManager;
    private final LikeDAO likeDAO;
    private final UserDAO userDAO;
    private final CommentDAO commentDAO;
    private final LabelDAO labelDAO;
    private final SavedPinDAO savedPinDAO;
    private final AuthenticationController permission;
    private final GamificationController gamification;
    private final NotificationsController notifications;
    private final ProxyConfig proxyConfig;


    @Autowired
    public PinsController(JdbcTemplate jdbcTemplate, PinDAO pinDAO, UserDAO userDAO,
                          CommentDAO commentDAO, LikeDAO likeDAO, LabelDAO labelDAO, SavedPinDAO savedPinDAO,
                          ElasticSearchClient elsClient, UploadsManager uploadsManager, AuthenticationController permission,
                          GamificationController gamification, NotificationsController notifications, ProxyConfig proxyConfig) {
        this.jdbcTemplate = jdbcTemplate;
        this.pinDAO = pinDAO;
        this.elsClient = elsClient;
        this.uploadsManager = uploadsManager;
        this.likeDAO = likeDAO;
        this.userDAO = userDAO;
        this.commentDAO = commentDAO;
        this.labelDAO = labelDAO;
        this.savedPinDAO = savedPinDAO;
        this.permission = permission;
        this.gamification = gamification;
        this.notifications = notifications;
        this.proxyConfig = proxyConfig;
        disableCertificateValidation(); // Needed for fetchWebPageContent to retrieve web page content for arbitrary HTTPS URLs
    }

    private void disableCertificateValidation() {
        TrustManager[] trustAllCerts = new TrustManager[]{
            new X509TrustManager() {
                public java.security.cert.X509Certificate[] getAcceptedIssuers() {
                    return null;
                }
                public void checkClientTrusted(
                        java.security.cert.X509Certificate[] certs, String authType) {
                }
                public void checkServerTrusted(
                        java.security.cert.X509Certificate[] certs, String authType) {
                }
            }
        };
        try {
            SSLContext sc = SSLContext.getInstance("SSL");
            sc.init(null, trustAllCerts, new java.security.SecureRandom());
            HttpsURLConnection.setDefaultSSLSocketFactory(sc.getSocketFactory());
        } catch (NoSuchAlgorithmException | KeyManagementException exception) {
            this.logger.error("Failed to disable certificate validation", exception);
        }
    }

    public void deleteAllPins() {
        this.jdbcTemplate.execute("TRUNCATE TABLE pins;");
    }

    @RequestMapping(value = "/{pin_id}", method = RequestMethod.GET)
    @ResponseBody
    @Valid
    public Pin getPinFromId(@PathVariable("pin_id") String pinId) {
        this.logger.debug("getFromId: pin_id={}", pinId);
        return this.pinDAO.findByPinId(pinId);
    }

    @RequestMapping(value = "/{pin_id}", method = RequestMethod.DELETE)
    @ResponseBody
    @Valid
    public int deletePinFromId(@PathVariable("pin_id") String pinId) {
        this.logger.debug("deletePinFromId: pinId={}", pinId);
        Pin pin = this.pinDAO.findByPinId(pinId);
        if (pin == null) {
            throw new VBoardException("No pin found with ID=" + pinId);
        }
        permission.ensureUserHasRightsToAlterPin(pin.getAuthor());
        try {
            this.elsClient.deletePin(pinId);
        } catch (VBoardException e) {
        } // NOPMD
        int result = pinDAO.removeByPinId(pinId);

        // Delete all likes on that pin
        List<Like> likes = this.likeDAO.findByPin(pinId);
        for (Like l : likes) {
            this.likeDAO.delete(l);
        }

        // Delete all comment on that pin
        List<Comment> comments = this.commentDAO.findByPin(pinId);
        for (Comment c : comments) {
            this.commentDAO.delete(c);
        }

        // Delete all savedPin which points on this pins
        List<SavedPin> savedPins = this.savedPinDAO.findByPinId(pinId);
        savedPins.forEach(this.savedPinDAO::delete);
        // Update the stats
        this.gamification.updateStats(permission.getSessionUserWithSyncFromDB());
        return result;
    }

    private String mediaType(String media) {
        if (isBlank(media)) {
            return "";
        }
        if (media.contains("</iframe>")) {
            return "video";
        }
        if (media.startsWith("http")) {
            return "internetImage";
        }
        if (media.startsWith("/pinImg/")) {
            return "base64ImageRef";
        }
        // Used string in DB for based64 imaged (not saved as a blob in DB) version compatibility
        if ("custom".equals(media)) {
            return "base64ImageRef";
        }
        return "base64Image"; // The default image is the base64
    }

    private boolean isMediaVideo(String media) {
        return "video".equals(mediaType(media));
    }

    private boolean isMediaBase64Image(String media) {
        return "base64Image".equals(mediaType(media));
    }

    private boolean isMediaInternetImage(String media) {
        return "internetImage".equals(mediaType(media));
    }

    @RequestMapping(value = "", method = RequestMethod.POST)
    @ResponseBody
    @Valid
    // Parsing the params in the JSON body requires using a dedicated @RequestBody annotated class instead of simple @RequestParam arguments
    public Pin addNewPin(@Valid @RequestBody AddNewPinParams params) {
        Pin retval;
        // Save
        String title = params.getTitle();
        String url = params.getUrl();
        String imgType = params.getImgType();

        if (!uploadsManager.isMultiplePinsPerUrlAllowed()) {
            Pin existingPin = this.pinDAO.findByHrefUrl(url);
            if (existingPin != null) {
                throw new DuplicateContentException("An existing pin already exists for this URL,"
                        + " created on " + existingPin.getPostDateUTC()
                        + " and with ID: " + existingPin.getPinId());
            }
        }

        String description = params.getDescription();
        String[] labels = params.getLabels();
        String strLabels = labels == null || labels.length == 0 ? "" : String.join(",", labels);
        String author = params.getAuthor();
        permission.ensureNewEntityAuthorMatchesSessionUser(author); // Check if the author given is effectively the one that posted the pin
        DateTime postDateUTC = new DateTime(DateTimeZone.UTC);
        Pin newPin = new Pin(title, url, 0, imgType, strLabels, description, author, postDateUTC);
        // If the media is an image (!<iframe) and is a base64image (custom) (version compatibility) the image points out on its localisation (relative url)
        if (this.isMediaBase64Image(imgType)) {
            String nasURL = "/pinImg/" + newPin.getPinId() + ".png";
            newPin.setImgType(nasURL);
        }
        // If the media is a video, the content is set as it is
        if (this.isMediaVideo(imgType)) {
            newPin.setImgType(imgType);
        }
        try {
            this.logger.debug("addNewPin: title={} - url={} - likes=0 - imgType={} - description={} - labels={} - author={}",
                    title, url, newPin.getImgType(), description, strLabels, author);
            retval = this.pinDAO.save(newPin);
            this.elsClient.addPin(newPin);
            // If the image given is not a video (iframe) and exists, the image is saved (if not url, see inside the method) in the NAS
            if (this.isMediaBase64Image(params.getImgType()) || this.isMediaInternetImage(params.getImgType())) {
                uploadsManager.savePinImage(params.getImgType(), retval.getPinId());
            }
            // Update the stats
            this.gamification.updateStats(permission.getSessionUserWithSyncFromDB());
        } catch (UnexpectedRollbackException e) {
            throw new VBoardException(e.getMessage(), e.getMostSpecificCause());
        }
        // Send a notification
        this.notifications.addNotificationsFromPin(newPin.getPinId(), "a ajouté une épingle avec un label que vous suivez");
        // Add the new labels in the list of labels
        if (!isBlank(strLabels)) {
            List<String> pinLabels = Arrays.asList(strLabels.split(","));
            pinLabels.forEach(l -> this.labelDAO.save(new Label(l)));
        }
        return retval;
    }

    // Post pins from Vblog (wordpress) (see previous method)
    @RequestMapping(value = "/vblog", method = RequestMethod.POST, consumes = {"application/x-www-form-urlencoded"})
    @ResponseBody
    @Valid
    public Pin addNewPinVblog(@RequestParam("title") final String title,
                              @RequestParam("url") final String url,
                              @RequestParam("imgType") String imgType,
                              @RequestParam("description") final String description,
                              @RequestParam("labels") String labels,
                              @RequestParam("author") String author,
                              @RequestParam("ID") final String ID) {


        // TODO only authorize wordpress (vblog) to access that url/method
        if (!this.isMediaInternetImage(imgType)) {
            imgType = null;
        }
        if (!isBlank(labels)) {
            labels = "#" + labels;
        }
        User user = this.userDAO.findByEmail(author);
        if (user != null) {
            author = user.getUserString();
        }
        Pin pin = this.pinDAO.findByPinId("vblog-" + ID);
        if (pin == null) {
            DateTime postDateUTC = new DateTime(DateTimeZone.UTC);
            pin = new Pin("vblog-" + ID, title, url, 0, imgType, labels, description, author, postDateUTC);
        } else {
            pin.setPinTitle(title);
            pin.setHrefUrl(url);
            pin.setImgType(imgType);
            pin.setLabels(labels);
            pin.setIndexableTextContent(description);
            pin.setAuthor(author);
        }
        try {
            this.logger.debug("addNewPinVblog: pinId= vblog-{} title={} - url={} - likes=0 - imgType={} - description={} - labels={} - author={}",
                    ID, title, url, imgType, description, labels, author);
            pin = this.pinDAO.save(pin);
            this.elsClient.updatePin(pin);
        } catch (UnexpectedRollbackException e) {
            throw new VBoardException(e.getMessage(), e.getMostSpecificCause());
        }
        // Add the new labels in the list of labels
        if (!isBlank(labels)) {
            List<String> pinLabels = Arrays.asList(labels.split(","));
            pinLabels.forEach(l -> this.labelDAO.save(new Label(l)));
        }
        if (this.isMediaInternetImage(imgType)) {
            uploadsManager.savePinImage(imgType, pin.getPinId());
        }
        if (user != null) {
            this.gamification.updateStats(user);
        }
        return pin;
    }

    // see addNewPin method
    @RequestMapping(value = "/update/{pinId}", method = RequestMethod.POST)
    @ResponseBody
    @Valid
    // Parsing the params in the JSON body requires using a dedicated @RequestBody annotated class instead of simple @RequestParam arguments
    public Pin updatePin(@Valid @RequestBody AddNewPinParams params, @PathVariable("pinId") String pinId) {
        Pin pin = this.pinDAO.findByPinId(pinId);
        if (pin == null) {
            throw new VBoardException("No pin found with ID=" + pinId);
        }
        permission.ensureUserHasRightsToAlterPin(pin.getAuthor());
        // Save
        pin.setPinTitle(params.getTitle());
        pin.setHrefUrl(params.getUrl());

        pin.setIndexableTextContent(params.getDescription());
        String[] labels = params.getLabels();
        String strLabels = labels == null || labels.length == 0 ? "" : String.join(",", labels);
        pin.setLabels(strLabels);
        if (this.isMediaBase64Image(params.getImgType())) {
            String nasURL = "/pinImg/" + pin.getPinId() + ".png";
            pin.setImgType(nasURL);
        }
        if (this.isMediaVideo(params.getImgType()) || this.isMediaInternetImage(params.getImgType())) {
            pin.setImgType(params.getImgType());
        }
        if (isBlank(this.mediaType(params.getImgType()))) {
            pin.setImgType(null);
        }
        try {
            this.logger.debug("UpdatePin: {}", pin);
            this.pinDAO.save(pin);
            this.elsClient.updatePin(pin);
            if (this.isMediaBase64Image(params.getImgType()) || this.isMediaInternetImage(params.getImgType()) /* && !this.isMediaBase64ImageRef(params.getImgType()) more explicit, but not necessary*/) {
                uploadsManager.savePinImage(params.getImgType(), pin.getPinId());
            }
        } catch (UnexpectedRollbackException e) {
            throw new VBoardException(e.getMessage(), e.getMostSpecificCause());
        }
        // Add the new labels in the list of labels
        if (!isBlank(strLabels)) {
            List<String> pinLabels = Arrays.asList(strLabels.split(","));
            pinLabels.forEach(l -> this.labelDAO.save(new Label(l)));
        }
        return pin;
    }

    // Get the base64 value of the image of the given pin (to use when no nas is SET, some code modification is needed)
    @RequestMapping(value = "/image/{pinId}", method = RequestMethod.GET, produces = "application/x-msdownload")
    // Very long string given, download allow big content to be sent
    @ResponseBody
    @Valid
    public String getImage(@PathVariable("pinId") String pinId) {
        this.logger.debug("getPinImage: pinId={}", pinId);
        return uploadsManager.getImage(pinId);
    }

    // Return elasticsearch Pin result according to parameters
    @RequestMapping(value = "", method = RequestMethod.GET)
    @ResponseBody
    @Valid
    public Iterable<Pin> search(@RequestParam(value = "text", required = false) String text,
                                @RequestParam(value = "labels", required = false) String labels,
                                @RequestParam(value = "from", required = false) String from,
                                @RequestParam(value = "offset", defaultValue = "0") Integer offset) {
        this.logger.debug("search: text={} - labels={} - from={} - offset={}",
                text, labels, from, offset);
        return this.elsClient.searchPins(text, labels, from, offset);
    }

    // Return elasticsearch Pin result according to parameters
    @RequestMapping(value = "/popular", method = RequestMethod.GET)
    @ResponseBody
    @Valid
    public Iterable<Pin> searchByLikes(@RequestParam(value = "text", required = false) String text,
                                       @RequestParam(value = "from", required = false) String from,
                                       @RequestParam(value = "offset", defaultValue = "0") int offset) {
        this.logger.debug("searchByLikes: text={} - from={} - offset={}",
                text, from, offset);
        return this.elsClient.searchPinsByLikes(text, from, offset);
    }

    // Return elasticsearch Pin result for a given author
    @RequestMapping(value = "/author", method = RequestMethod.GET)
    @ResponseBody
    @Valid
    public Iterable<Pin> searchByAuth(@RequestParam(value = "author", required = false) String author,
                                      @RequestParam(value = "from", required = false) String from) {
        this.logger.debug("search: author={} - from={}", author, from);
        return this.elsClient.searchPinsByAuthor(author, from);
    }

    // Return elasticsearch Pin result for a given pinId
    @RequestMapping(value = "/id/{pinId}", method = RequestMethod.GET)
    @ResponseBody
    @Valid
    public Iterable<Pin> searchById(@PathVariable("pinId") String pinId) {
        this.logger.debug("search: pinId={}", pinId);
        return this.elsClient.searchPinsById(pinId);
    }

    // Add add newsletter tag or remove it if already there for a given pin
    @RequestMapping(value = "/toggleNewsletterLabel/{pinId}", method = RequestMethod.POST)
    @ResponseBody
    @Valid
    public Pin updatePinLabelNL(@PathVariable("pinId") String pinId) {
        Pin pin = this.pinDAO.findByPinId(pinId);
        User u = permission.getSessionUser();
        String label = "#newsletter";
        if (u.getNewsletterLabel() != null && u.getNewsletterLabel().length() > 1) {
            label = u.getNewsletterLabel();
        }
        permission.ensureUserHasNewsletterRole();
        this.labelDAO.save(new Label(label));
        List<String> labels = pin.getLabelsAsList();
        if (labels.size() == 1 && isBlank(labels.get(0))) {
            pin.setLabels(label);
            this.pinDAO.save(pin);
            this.elsClient.updatePin(pin);
            return pin;
        }
        if (labels.contains(label)) {
            labels.remove(label);
        } else {
            labels.add(label);
        }
        pin.setLabels(String.join(",", labels));
        this.pinDAO.save(pin);
        this.elsClient.updatePin(pin);
        return pin;
    }

    // Return the most used labels for the current user, which means return a list of labels which have been used more
    // than 33% of the time together.
    @RequestMapping(value = "/getMostUsedLabels", method = RequestMethod.GET, produces = "plain/text")
    @ResponseBody
    @Valid
    public String getMostUsedLabelsIfSignificant() {
        HashMap<String, Integer> authorLabelsCounter = new HashMap<>();
        List<Pin> pins = this.pinDAO.findByAuthor(permission.getSessionUser().getUserString()); // Get all pins created by the user
        // To avoid error throwing with foreach
        if (pins == null || pins.isEmpty()) {
            return "";
        }
        pins.forEach(p -> {
            if (!p.getLabels().isEmpty()) {
                String key = String.join(",", p.getSortedLabels()); // Sort the labels of the pin alphabetically
                if (authorLabelsCounter.containsKey(key)) {
                    authorLabelsCounter.put(key, authorLabelsCounter.get(key) + 1); // Increase the value of the hashmap if the labels (key) is already there
                } else {
                    authorLabelsCounter.put(key, 1); // Or set a new key (labels sorted) with the number of appearance: 1
                }
            }
        });
        if (authorLabelsCounter.isEmpty()) {
            return "";
        }
        if (authorLabelsCounter.size() == 1) {
            return authorLabelsCounter.entrySet().iterator().next().getKey();
        }
        String mostUsedLabels = Collections.max(authorLabelsCounter.entrySet(), Comparator.comparingInt(Map.Entry::getValue)).getKey(); // Get the max used labels
        if (authorLabelsCounter.get(mostUsedLabels) > pins.size() / 3) { // Check if it has been used more than 1 on 3 times
            return mostUsedLabels;
        } else {
            return "";
        }
    }

    // Small manual method to scrap given url to retrieve the title, an image and a description of a page
    // A library can be used more efficiently
    @RequestMapping(value = "/url", method = RequestMethod.POST)
    @ResponseBody
    @Valid
    public String getURL(@Valid @RequestBody String urlInfo) {
        final String url = JavaUtils.extractJSONObject(urlInfo, "urlinfo");
        JSONObject info = new JSONObject();
        try {
            info.put("title", "");
            info.put("image", "");
            info.put("description", "");
            String html;
            try {
                html = fetchWebPageContent(url);
            } catch (VBoardException e) {
                logger.error("Could not fetch web page to extract info for url: " + url, e);
                return info.toString();
            }

            /* Retrieve the html code between the title tags */
            final Pattern patternTitle = Pattern.compile("<title>(.*?)</title>");
            final Matcher matcherTitle = patternTitle.matcher(html);
            if (matcherTitle.find()) {
                info.put("title", matcherTitle.group(1).trim());
            }

            /* Retrieve the html code between the body tags */
            final Pattern patternBody = Pattern.compile("<body(.*?)</body>");
            final Matcher matcherBody = patternBody.matcher(html);
            String body = "";
            if (matcherBody.find()) {
                body = matcherBody.group(1);
            }

            /* Retrieve the html code between the img tags */
            String imgSrcUrl = "";
            final Pattern patternImage = Pattern.compile("<img(.*?)>");
            final Matcher matcherImage = patternImage.matcher(body);
            while (matcherImage.find()) { // Try to find a suitable image (we stop when no more images are found or if one is found)
                String imgTag = matcherImage.group(1);
                /* Retrieve the image's source */
                Pattern patternSrc = Pattern.compile("src=\"(.*?)\"");
                Matcher matcherSrc = patternSrc.matcher(imgTag);
                if (matcherSrc.find()) {
                    imgSrcUrl = matcherSrc.group(1);
                    info.put("image", imgSrcUrl);
                    break;
                }
            }

            /* Retrieve the html code just after the previous image found or if not found, just after the h1 tag */
            String startingPoint = imgSrcUrl;
            if (isBlank(startingPoint)) {
                startingPoint = "<h1>";
            }
            final Pattern patternMain = Pattern.compile(startingPoint + "(.*?)</body");
            final Matcher matcherMain = patternMain.matcher(html);
            String main = "";
            if (matcherMain.find()) {
                main = matcherMain.group(1);
            }

            /* Retrieve the html code between the p tag inside the previous code */
            final Pattern patternDesc = Pattern.compile("<p(.*?)</p>");
            final Matcher matcherDesc = patternDesc.matcher(main);
            String description = "";
            if (matcherDesc.find()) {
                /* Remove all empty spaces (double ones) and check that the length is not too smal (avoid empty tags od adds) */
                description = matcherDesc.group(1).replace("  ", " ");
                while (description.length() < 150 && matcherDesc.find()) {
                    description = matcherDesc.group(1).replace("  ", " ");
                }
            }

            /* Remove HTML tags */
            final Pattern patternTag = Pattern.compile("<(.*?)>");
            final Matcher matcherTag = patternTag.matcher(description);
            description = matcherTag.replaceAll("");
            // > : The tag of the linked image (linked to the p tag) is not replaced because the open < is not there
            description = description.substring(description.indexOf('>') + 1);

            // Limit the description to 254 characters (DB is set to VARCHAR, which is limited to 255 characters)
            if (description.length() > 255) {
                description = description.substring(0, 250) + "...";
            }

            info.put("description", description);
        } catch (JSONException jsonException) {
            this.logger.error("Could not add key to JSON object: {}", jsonException);
        }
        return info.toString();
    }

    private String fetchWebPageContent(final String url) {
        URL path;
        try {
            path = new URL(url);
        } catch (MalformedURLException e) {
            throw new VBoardException("Invalid URL", e);
        }
        try {
            BufferedReader buffer = new BufferedReader(
                    new InputStreamReader(path.openConnection(proxyConfig.getProxy()).getInputStream(), UTF_8)
            );
            return IOUtils.toString(buffer);
        } catch (IOException e) {
            throw new VBoardException("Not able to read web page from url", e);
        }
    }
}
