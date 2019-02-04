package com.example.web.rest;

import com.example.domain.Demo;
import com.example.domain.DemoAgePatch;
import com.example.repository.DemoRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.NullNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.rest.webmvc.ResourceNotFoundException;
import org.springframework.data.rest.webmvc.json.patch.JsonPatchPatchConverter;
import org.springframework.data.rest.webmvc.json.patch.Patch;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/demos")
public class DemoResource {

    private final ObjectMapper objectMapper;

    private final DemoRepository demoRepository;

    public DemoResource(ObjectMapper objectMapper, DemoRepository demoRepository) {
        this.objectMapper = objectMapper;
        this.demoRepository = demoRepository;
    }

    /**
     * Get all demos.
     * <p>
     * cURL: <code>curl -v "http://localhost:8080/api/demos"</code>
     */
    @GetMapping
    public ResponseEntity<List<Demo>> getAllDemos() {
        log.info("GET request");
        List<Demo> result = demoRepository.findAll();
        log.info("GET result: {}", result);
        return ResponseEntity.ok().body(result);
    }

    /**
     * Create a new demo.
     * <p>
     * cURL: <code>curl -X POST -v "http://localhost:8080/api/demos" --header "Content-Type: application/json" --data "{\"firstName\":\"John\",\"lastName\":\"Doe\",\"age\":20}"</code>
     */
    @PostMapping
    public ResponseEntity<Demo> createDemo(@RequestBody @Valid Demo request) throws URISyntaxException {
        log.info("POST request: {}", request);
        Demo result = demoRepository.save(request);
        log.info("POST result: {}", result);
        return ResponseEntity.created(new URI("/api/demos/" + result.getId())).body(result);
    }

    /**
     * Patch an existing demo using a partial demo entity.
     * <p>
     * The PATCH method requests that a set of changes described in the
     * request entity be applied to the resource identified by the Request-
     * URI.  The set of changes is represented in a format called a "patch
     * document" identified by a media type.  If the Request-URI does not
     * point to an existing resource, the server MAY create a new resource,
     * depending on the patch document type (whether it can logically modify
     * a null resource) and permissions, etc.
     * <p>
     * PATCH is neither safe nor idempotent as defined by {@link <a href="https://tools.ietf.org/html/rfc2616">[RFC2616]</a>}, Section
     * 9.1.
     * <p>
     * A PATCH request can be issued in such a way as to be idempotent,
     * which also helps prevent bad outcomes from collisions between two
     * PATCH requests on the same resource in a similar time frame.
     * Collisions from multiple PATCH requests may be more dangerous than
     * PUT collisions because some patch formats need to operate from a
     * known base-point or else they will corrupt the resource.  Clients
     * using this kind of patch application SHOULD use a conditional request
     * such that the request will fail if the resource has been updated
     * since the client last accessed the resource.  For example, the client
     * can use a strong ETag [RFC2616] in an If-Match header on the PATCH
     * request.
     *
     * @see <a href="https://tools.ietf.org/html/rfc5789">RFC5789</a>
     * <p>
     * cURL: <code>curl -X PATCH -v "http://localhost:8080/api/demos/1" --header "Content-Type: application/json" --data "{\"age\":\"30\"}"</code>
     */
    @PatchMapping(value = "{id}")
    public ResponseEntity<Demo> patchDemo(@PathVariable Long id, @RequestBody @Valid DemoAgePatch request) {
        log.info("PATCH request: {}", request);
        Demo result = demoRepository.save(id, request);
        log.info("PATCH result: {}", result);
        return ResponseEntity.ok().body(result);
    }

    /**
     * Patch an existing demo using a custom JSON merge.
     * <p>
     * Create a JsonMergePatch from the ObjectNode.
     * <p>
     * A JSON merge patch document describes changes to be made to a target
     * JSON document using a syntax that closely mimics the document being
     * modified.  Recipients of a merge patch document determine the exact
     * set of changes being requested by comparing the content of the
     * provided patch against the current content of the target document.
     * If the provided merge patch contains members that do not appear
     * within the target, those members are added.  If the target does
     * contain the member, the value is replaced.  Null values in the merge
     * patch are given special meaning to indicate the removal of existing
     * values in the target.
     *
     * @see <a href="https://tools.ietf.org/html/rfc7386">RFC7386</a>
     * <p>
     * cURL: <code>curl -X PATCH -v "http://localhost:8080/api/demos/1" --header "Content-Type: application/merge-patch+json" --data "{\"age\":30}"</code>
     */
    @PatchMapping(value = "{id}", consumes = "application/merge-patch+json")
    public ResponseEntity<Demo> jsonMergePatchDemo(@PathVariable Long id, @RequestBody JsonNode request) {
        log.info("JSON MERGE request: {}", request);
        Demo demo = demoRepository.findById(id).orElseThrow(ResourceNotFoundException::new);
        JsonNode demoJson = objectMapper.convertValue(demo, JsonNode.class);
        JsonNode jsonResult = merge(demoJson, request);
        log.info("JSON result: {}", jsonResult);
        Demo result = objectMapper.convertValue(jsonResult, Demo.class);
        demoRepository.save(result);
        log.info("JSON MERGE result: {}", result);
        return ResponseEntity.ok().body(result);
    }

    /**
     * Merge JSON objects.
     *
     * To apply the merge patch document to a target resource, the system
     * realizes the effect of the following function, described in
     * pseudocode.
     * The original pseudocode can be found in the RFC7386 document.
     *
     * @see <a href="https://tools.ietf.org/html/rfc7386#section-2">RFC7386</a>
     * <p>
     * <pre>
     *     define MergePatch(Target, Patch):
     *         if Patch is an Object:
     *             if Target is not an Object:
     *                 Target = {} # Ignore the contents and set it to an empty Object
     *             for each Name/Value pair in Patch:
     *                 if Value is null:
     *                     if Name exists in Target:
     *                         remove the Name/Value pair from Target
     *                 else:
     *                     Target[Name] = MergePatch(Target[Name], Value)
     *             return Target
     *         else:
     *             return Patch
     * </pre>
     */
    private JsonNode merge(JsonNode targetNode, JsonNode patchNode) {
        if (patchNode instanceof ObjectNode) {
            ObjectNode targetObject =
                targetNode instanceof ObjectNode ? (ObjectNode) targetNode :
                JsonNodeFactory.instance.objectNode();

            patchNode.fields().forEachRemaining(entry -> {
                if (entry.getValue() instanceof NullNode) {
                    if (targetObject.has(entry.getKey())) {
                        targetObject.remove(entry.getKey());
                    }
                } else {
                    JsonNode result = merge(targetObject.get(entry.getKey()), entry.getValue());
                    targetObject.replace(entry.getKey(), result);
                }
            });
            return targetObject;
        } else {
            return patchNode;
        }
    }

    /**
     * Patch existing demo using Spring MVC JSON patch.
     * <p>
     * Create a JsonPatch from the ArrayNode.
     * <p>
     * JSON Patch is a format (identified by the media type "application/
     * json-patch+json") for expressing a sequence of operations to apply to
     * a target JSON document; it is suitable for use with the HTTP PATCH
     * method.
     * <p>
     * This format is also potentially useful in other cases in which it is
     * necessary to make partial updates to a JSON document or to a data
     * structure that has similar constraints (i.e., they can be serialized
     * as an object or an array using the JSON grammar).
     *
     * @see <a href="https://tools.ietf.org/html/rfc6902">RFC6902</a>
     * <p>
     * cURL: <code>curl -X PATCH -v "http://localhost:8080/api/demos/1" --header "Content-Type: application/json-patch+json" --data "[{\"op\":\"replace\",\"path\":\"age\",\"value\":\"30\"}]"</code>
     */
    @PatchMapping(value = "{id}", consumes = "application/json-patch+json")
    public ResponseEntity<Demo> jsonPatchDemo(@PathVariable Long id, @RequestBody byte[] request) throws IOException {
        log.info("JSON PATCH request: {}", request);
        Patch patch = new JsonPatchPatchConverter(objectMapper).convert(objectMapper.readTree(request));
        Demo demo = demoRepository.findById(id).orElseThrow(ResourceNotFoundException::new);
        Demo result = patch.apply(demo, Demo.class);
        demoRepository.save(result);
        log.info("JSON PATCH result: {}", result);
        return ResponseEntity.ok().body(result);
    }
}
