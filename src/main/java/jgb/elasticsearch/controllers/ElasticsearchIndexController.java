package jgb.elasticsearch.controllers;

import jgb.elasticsearch.utils.Constants;
import org.apache.log4j.Logger;
import org.apache.lucene.search.join.ScoreMode;
import org.elasticsearch.action.ActionListener;
import org.elasticsearch.action.DocWriteRequest;
import org.elasticsearch.action.admin.cluster.health.ClusterHealthResponse;
import org.elasticsearch.action.admin.indices.create.CreateIndexResponse;
import org.elasticsearch.action.admin.indices.exists.indices.IndicesExistsResponse;
import org.elasticsearch.action.bulk.BulkResponse;
import org.elasticsearch.action.index.IndexResponse;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.action.search.SearchType;
import org.elasticsearch.action.support.WriteRequest;
import org.elasticsearch.client.Requests;
import org.elasticsearch.client.transport.TransportClient;
import org.elasticsearch.common.io.Streams;
import org.elasticsearch.common.unit.TimeValue;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.common.xcontent.json.JsonXContent;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.joda.time.LocalDate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import java.io.ByteArrayOutputStream;
import java.util.UUID;

/**
 * @author jgb
 * @since 6/15/17 12:03 PM
 */
@RestController
@RequestMapping("/elastic/index")
public class ElasticsearchIndexController {

    private static final Logger LOG = Logger.getLogger(ElasticsearchIndexController.class);

    @Autowired
    private TransportClient client;
    @Value("classpath:elasticsearch/catalog-index.json")
    private Resource index;

    @RequestMapping(value = "/{indexName}/exists", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<IndicesExistsResponse> indexExists(@PathVariable String indexName) {
        final IndicesExistsResponse response = client
                .admin()
                .indices()
                .prepareExists(indexName)
                .get(TimeValue.timeValueMillis(100));

        LOG.info("Index " + indexName + " exists: " + response.isExists());
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    @RequestMapping(value = "/catalog", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<CreateIndexResponse> createIndexCatalog() {
        try (final ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Streams.copy(index.getInputStream(), out);

            final CreateIndexResponse response = client
                    .admin()
                    .indices()
                    .prepareCreate(Constants.Elastic.INDEX_CATALOG)
                    .setSource(out.toByteArray())
                    .setTimeout(TimeValue.timeValueSeconds(1))
                    .get(TimeValue.timeValueSeconds(2));

            LOG.info("Success creating index: " + response.isShardsAcked());
            return new ResponseEntity<>(response, HttpStatus.CREATED);
        } catch (Exception e) {
            LOG.error("Exception.", e);
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
}