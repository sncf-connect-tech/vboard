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

package com.vsct.vboard.services;

import com.vsct.vboard.config.ElasticSearchClientConfig;
import com.vsct.vboard.models.Pin;
import com.vsct.vboard.models.VBoardException;
import com.vsct.vboard.utils.ValidatorUtils;
import io.searchbox.client.JestClientFactory;
import io.searchbox.client.JestResult;
import io.searchbox.client.config.HttpClientConfig;
import io.searchbox.client.http.JestHttpClient;
import io.searchbox.core.Delete;
import io.searchbox.core.Index;
import io.searchbox.core.Search;
import io.searchbox.core.SearchResult;
import io.searchbox.core.Update;
import io.searchbox.core.search.sort.Sort;
import io.searchbox.params.Parameters;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

import static java.lang.Boolean.TRUE;

@Service
public class ElasticSearchClient {
    private final ElasticSearchClientConfig elsConfig;
    private JestHttpClient elasticSearchClient = null;

    @Autowired
    ElasticSearchClient(ElasticSearchClientConfig elsConfig) {
        this.elsConfig = elsConfig;
    }

    // Lazy initialization because we want to be able to instantiate this bean without an ElasticSearch server beeing available
    public JestHttpClient lazyGetElsClient() {
        if (this.elasticSearchClient == null) {
            final JestClientFactory factory = new JestClientFactory();
            factory.setHttpClientConfig(new HttpClientConfig
                    .Builder(this.elsConfig.getServerUri())
                    .multiThreaded(true)
                    .build());
            this.elasticSearchClient = (JestHttpClient) factory.getObject();
        }
        return this.elasticSearchClient;
    }

    /**
     * Pin Search
     *
     * @param text
     * @param labels
     * @param fromDate
     * @param offset
     * @return
     */
    private List<Pin> searchAndSortPins(String text, List<String> labels, String fromDate, int offset, Sort sort) {
        String query = getSearchPinsQuery(text, labels, fromDate);
        List<Sort> sortList = new LinkedList<>();
        sortList.add(sort);
        List<HashMap> items = this.search(query, this.elsConfig.getPinsIndex(), sortList, offset);
        return items.stream().map(this::jsonMapToPin).collect(Collectors.toList());
    }

    /**
     * Pin search sorted by date
     *
     * @param text
     * @param labels
     * @param fromDate
     * @param offset
     * @return
     */
    public List<Pin> searchPins(String text, String labels, String fromDate, int offset) {
        Sort sort = new Sort("post_date_utc", Sort.Sorting.DESC);
        return this.searchAndSortPins(text == null ? "" : text,
                labels == null ? Collections.emptyList() : Arrays.asList(labels.split(", *")),
                fromDate, offset, sort);
    }


    /**
     * Pin search sorted by popularity (likes)
     *
     * @param text
     * @param fromDate
     * @param offset
     * @return
     */
    public List<Pin> searchPinsByLikes(String text, String fromDate, int offset) {
        Sort sort = new Sort("likes", Sort.Sorting.DESC);
        return this.searchAndSortPins(text == null ? "" : text,
                Collections.emptyList(),
                fromDate, offset, sort);
    }

    /**
     * Pin search posted by a given user
     *
     * @param author
     * @param from
     * @return
     */
    public List<Pin> searchPinsByAuthor(String author, String from) {
        String query = getSearchPinsQuerybyAuthor(author, from);
        List<Sort> sortList = new LinkedList<>();
        Sort sort = new Sort("post_date_utc", Sort.Sorting.DESC);
        sortList.add(sort);
        List<HashMap> items = this.search(query, this.elsConfig.getPinsIndex(), sortList, 0);
        return items.stream().map(this::jsonMapToPin).collect(Collectors.toList());
    }

    /**
     * Pin search with a given Id
     *
     * @param pinId
     * @return
     */
    public List<Pin> searchPinsById(String pinId) {
        String query = getSearchPinsQuerybyId(pinId);
        List<Sort> sortList = new LinkedList<>();
        List<HashMap> items = this.search(query, this.elsConfig.getPinsIndex(), sortList, 0);
        return items.stream().map(this::jsonMapToPin).collect(Collectors.toList());
    }

    public void deletePin(String pinId) {
        try {
            final JestResult result = this.lazyGetElsClient().execute(new Delete.Builder(pinId).index(this.elsConfig.getPinsIndex()).type("jdbc").build());
            if (!result.isSucceeded()) {
                throw new VBoardException("Pin deletion from ID failed: " + result.getErrorMessage());
            }
        } catch (IOException e) {
            throw new VBoardException("Pin deletion from ID failed", e);
        }

    }

    private void changeLikeCount(String pinId, String diff) {
        final String script = "{ \"script\" : \"ctx._source.likes += " + diff + "\" }";
        try {
            final JestResult result = this.lazyGetElsClient().execute(new Update.Builder(script).index(this.elsConfig.getPinsIndex()).type("jdbc").id(pinId).build());
            if (!result.isSucceeded()) {
                throw new VBoardException("Pin likes count increment failed: " + result.getErrorMessage());
            }
        } catch (IOException e) {
            throw new VBoardException("Pin likes count increment failed", e);
        }
    }

    public void addLike(String pinId) {
        this.changeLikeCount(pinId, "1");
    }

    public void removeLike(String pinId) {
        this.changeLikeCount(pinId, "-1");
    }

    private void changeCommentCount(String pinId, String diff) {
        final String script = "{ \"script\" : \"ctx._source.comments_number += " + diff + "\" }";
        try {
            final JestResult result = this.lazyGetElsClient().execute(new Update.Builder(script).index(this.elsConfig.getPinsIndex()).type("jdbc").id(pinId).build());
            if (!result.isSucceeded()) {
                throw new VBoardException("Pin comments count increment failed: " + result.getErrorMessage());
            }
        } catch (IOException e) {
            throw new VBoardException("Pin comments count increment failed", e);
        }
    }

    public void addComment(String pinId) {
        this.changeCommentCount(pinId, "1");
    }

    public void removeComment(String pinId) {
        this.changeCommentCount(pinId, "-1");
    }

    public void updatePin(Pin pin) {
        final String script = "{ \"script\" : \"ctx._source = pinUpdate\", \"params\": { \"pinUpdate\": " + pin + " }}";
        try {
            final JestResult result = this.lazyGetElsClient().execute(new Update.Builder(script).index(this.elsConfig.getPinsIndex()).type("jdbc").id(pin.getPinId()).build());
            if (!result.isSucceeded()) {
                throw new VBoardException("Pin update failed: " + result.getErrorMessage());
            }
        } catch (IOException e) {
            throw new VBoardException("Pin update failed", e);
        }
    }

    public void addPin(Pin pin) {
        try {
            final JestResult result = this.lazyGetElsClient().execute(new Index.Builder(pin.toString()).index(this.elsConfig.getPinsIndex()).type("jdbc").id(pin.getPinId()).build());
            if (!result.isSucceeded()) {
                throw new VBoardException("Pin creation failed: " + result.getErrorMessage());
            }
        } catch (IOException e) {
            throw new VBoardException("Pin creation failed", e);
        }
    }

    private String getSearchPinsQuery(String text, List<String> labels, String from) {
        text = text.toLowerCase();
        // Search field full text
        String searchTextMultiFieldsShould = "";
        String searchTagsMust = "";
        String searchFrom = "";
        if (!text.isEmpty()) {
            searchTextMultiFieldsShould = "\"should\": [ {"
                    + "\"wildcard\": { \"indexable_text_content\": \"*" + text + "*\" }"
                + " }, {"
                    + "\"wildcard\": { \"author\": \"*" + text + "*\" }"
                + " }, {"
                    + "\"wildcard\": { \"pin_title\": \"*" + text + "*\" }"
                + "} ]";
        }
        if (!labels.isEmpty()) {
            if (!searchTextMultiFieldsShould.isEmpty()) {
                searchTagsMust = ", ";
            }
            // The request in a "OR": we get all pins having any of the labels provided
            searchTagsMust += "\"should\": [ {" + labels.stream().map(label ->
                    "\"wildcard\" : {\"labels\": \"*" + label.toLowerCase() + "*\"}"
            ).collect(Collectors.joining(" }, {")) + "} ]";
        }
        if (from != null && !from.isEmpty()) {
            if (!searchTextMultiFieldsShould.isEmpty() || !searchTagsMust.isEmpty()) {
                searchFrom = ", ";
            }
            searchFrom += "\"filter\": { \"range\": { \"post_date_utc\": { \"from\":\"" + from + "\" } }}";
        }
        return "{" +
                    "\"query\": {" +
                        "\"bool\": {" +
                            searchTextMultiFieldsShould +
                            searchTagsMust +
                            searchFrom +
                        "}" +
                    "}" +
                "}";
        // For CURL tests, the curl to post has the following format:
        //$ curl -XPOST 'http://localhost:9200/jdbc_pins_index/_search?pretty' -d '{"query": { "bool": { "should": [ {"match_all" : { } }, {"match_all" : { } }]}}, "filter": { "range": { "post_date_utc": { "from":"2014-03-21" } }}}'
        //$ curl -XPOST 'http://localhost:9200/jdbc_pins_index/_search?pretty' -d '{"query": { "bool": { "should": [ {"wildcard": { "indexable_text_content": "*text*" } }, {"wildcard": { "author": "*text*" } }]}}, "filter": { "range": { "post_date_utc": { "from":"2014-03-21" } }}}'
    }

    /**
     * Query for the search by author
     *
     * @param author
     * @param from
     * @return
     */
    private String getSearchPinsQuerybyAuthor(String author, String from) {
        // Search field full text
        author = author.toLowerCase();
        String searchAuth = "\"match_all\" : { }";
        if (StringUtils.isNotEmpty(author)) {
            searchAuth = "\"term\": { \"author\": \"" + author + "\" }";
        }
        String searchFrom = "";
        if (StringUtils.isNotEmpty(from)) {
            searchFrom = ", \"filter\": { \"range\": { \"post_date_utc\": { \"from\":\"" + from + "\" } }}";
        }
        return "{" +
                    "\"query\": {" +
                        "\"bool\": {" +
                            "\"must\":  { " + searchAuth + " }" +
                            searchFrom +
                        "}" +
                    "}" +
                "}";
    }

    /**
     * Query for the Search by id
     *
     * @param pinId
     * @return
     */
    private String getSearchPinsQuerybyId(String pinId) {
        String searchId = "\"match_all\" : { }";
        if (StringUtils.isNotEmpty(pinId)) {
            searchId = "\"term\": { \"_id\": \"" + pinId + "\" }";
        }
        return "{" +
            "\"query\": {" +
                "\"bool\": {" +
                    "\"must\":  { " + searchId + " }" +
                "}" +
            "}" +
        "}";
    }

    private List<HashMap> search(String query, String indexName, List<Sort> sortList, int offset) {
        Search search = this.buildSearch(query, indexName, sortList, offset);
        SearchResult elsResult;
        try {
            elsResult = this.lazyGetElsClient().execute(search);
        } catch (IOException error) {
            throw new ElsRequestError(error);
        }
        if (!elsResult.isSucceeded()) {
            throw new ElsRequestError("ElasticSearch failed: " + elsResult.getErrorMessage());
        }
        return getResponseAsList(elsResult);
    }

    // Create the jest (elasticSearch for Java) search query
    private Search buildSearch(String query, String indexName, Collection<Sort> sortList, int offset) {
        return new Search.Builder(query)
                .addIndex(indexName)
                .setParameter(Parameters.SIZE, this.elsConfig.getQuerySize())
                .setParameter("from", offset)
                .setParameter(Parameters.EXPLAIN, TRUE)
                .addSort(sortList)
                .build();
    }

    public List<HashMap> getResponseAsList(SearchResult elsResult) {
        return elsResult.getHits(HashMap.class).stream()
                .map(hit -> hit.source)
                .collect(Collectors.toList());
    }

    private Pin jsonMapToPin(HashMap jsonMap) {
        final Pin pin = new Pin((String) jsonMap.get("pin_id"),
                                (String) jsonMap.get("pin_title"),
                                (String) jsonMap.get("href_url"),
                                // ElasticSearch or jsonMap seems to integrate doubles by default whereas the parameter is an int
                                jsonMap.get("likes") != null ? ((Double) jsonMap.get("likes")).intValue() : 0,
                                (String) jsonMap.get("img_type"),
                                (String) jsonMap.get("labels"),
                                (String) jsonMap.get("indexable_text_content"),
                                (String) jsonMap.get("author"),
                                (String) jsonMap.get("post_date_utc"),
                                jsonMap.get("comments_number") != null ? ((Double) jsonMap.get("comments_number")).intValue() : 0);
        ValidatorUtils.validate(pin);
        return pin;
    }

    public static class ElsRequestError extends RuntimeException {
        ElsRequestError(String msg) {
            super(msg);
        }

        ElsRequestError(Throwable cause) {
            super(cause);
        }
    }
}
