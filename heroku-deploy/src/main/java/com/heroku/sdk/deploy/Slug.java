package com.heroku.sdk.deploy;


import org.apache.commons.lang3.StringEscapeUtils;

import java.io.*;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.Map;

public class Slug {

  private String blobUrl;

  private String slugId;

  private String stackName;

  private String createJson;

  private String appName;

  private String commit;

  private Map<String,String> headers;

  public static final String BASE_URL = "https://api.heroku.com";

  public Slug(String buildPackDesc, String appName, String stack, String commit, String encodedApiKey, Map<String,String> processTypes) throws UnsupportedEncodingException {
    this.appName = appName;

    headers = new HashMap<String,String>();
    headers.put("Authorization", encodedApiKey);
    headers.put("Content-Type", "application/json");
    headers.put("Accept", "application/vnd.heroku+json; version=3");

    createJson = "{" +
        "\"buildpack_provided_description\":\"" + StringEscapeUtils.escapeJson(buildPackDesc) +"\"," +
        "\"stack\":\"" + (stack == null ? "cedar-14" : StringEscapeUtils.escapeJson(stack)) +"\"," +
        "\"commit\":\"" + (commit == null ? "" : StringEscapeUtils.escapeJson(commit)) +"\"," +
        "\"process_types\":{";

    boolean first = true;
    for (String key : processTypes.keySet()) {
      String value = processTypes.get(key);
      if (!first) createJson += ", ";
      first = false;
      createJson += "\"" + key + "\"" + ":" + "\"" + StringEscapeUtils.escapeJson(value) + "\"";
    }
    createJson +=  "}}";
  }

  public String getBlobUrl() { return blobUrl; }
  public String getSlugId() { return slugId; }
  public String getStackName() { return stackName; }
  public String getSlugRequest() { return createJson; }
  public String getCommit() { return commit; }

  public Map create() throws IOException {
    String urlStr = BASE_URL + "/apps/" + URLEncoder.encode(appName, "UTF-8") + "/slugs";
    Map slugResponse = RestClient.post(urlStr, createJson, headers);

    Map blobJson = (Map)slugResponse.get("blob");
    blobUrl = (String)blobJson.get("url");

    slugId = (String)slugResponse.get("id");

    commit = (String)slugResponse.get("commit");

    Map stackJson = (Map)slugResponse.get("stack");
    stackName = (String)stackJson.get("name");

    return slugResponse;
  }

  public void upload(File slugFile) throws IOException {
    if (blobUrl == null) {
      throw new IllegalStateException("Slug must be created before uploading!");
    }

    RestClient.put(blobUrl, slugFile);
  }

  public Map release() throws IOException {
    if (slugId == null) {
      throw new IllegalStateException("Slug must be created before releasing!");
    }

    String urlStr = BASE_URL + "/apps/" + appName + "/releases";

    String data = "{\"slug\":\"" + slugId + "\"}";

    return RestClient.post(urlStr, data, headers);
  }
}
