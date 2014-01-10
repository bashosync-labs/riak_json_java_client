package com.basho.riak.json.jackson;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import com.basho.riak.json.Schema;
import com.basho.riak.json.errors.RJSerializationError;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import static com.basho.riak.json.utils.StreamUtils.rewindStream;

/**
 * Implements the serialization concern.
 * 
 * @author Randy Secrist
 */
public class DefaultSerializer implements Serialization {
  
  // http://wiki.fasterxml.com/JacksonFAQThreadSafety
  private static final ObjectMapper mapper = new ObjectMapper();
    
  public String toJsonString(JsonSerializable object) {
    if (object == null)
      return null;
    String rtnval = null;
    try {
      rtnval = mapper.writeValueAsString(object);
    }
    catch (JsonProcessingException e) {
      throw unexpectedWriteFailure(object, e);
    }
    return rtnval;
  }
  
  public void toOutputStream(JsonSerializable json, OutputStream stream) {
    try {
      mapper.writeValue(stream, json);
    }
    catch (IOException e) {
      throw unexpectedIOFailure(e);
    }
  }
  
  public Schema fromJsonString(String json) {
      Schema rtnval = null;
      try {
        rtnval = mapper.readValue(json, Schema.class);
      }
      catch (JsonMappingException | JsonParseException | RuntimeException e) {
        throw unexpectedReadFailure(json.getBytes(), e);
      }
      catch (IOException e) {
    	throw unexpectedIOFailure(e);
      }
      return rtnval;
  }
  
  public Schema fromInputStream(InputStream stream) {
      Schema rtnval = null;
      try {
        rtnval = mapper.readValue(stream, Schema.class);
      }
      catch (JsonMappingException | JsonParseException | RuntimeException e) {
        throw unexpectedReadFailure(rewindStream(stream), e);
      }
      catch (IOException e) {
        throw unexpectedReadFailure(rewindStream(stream), e);
      }
      return rtnval;
  }
  
  private RJSerializationError unexpectedIOFailure(IOException ioe) {
    return new RJSerializationError(
      "Unexpected failure:" + " [ " + ioe.getClass().getSimpleName() + " ], " + ioe.getLocalizedMessage(), ioe);
  }
   
  private RJSerializationError unexpectedWriteFailure(JsonSerializable type, Throwable cause) {
    return new RJSerializationError(
      "Unexpected serialization type:" + " [ " + type.getClass().getSimpleName() + " ] ", cause);
  }

  private RJSerializationError unexpectedReadFailure(byte[] serialized_data, Throwable cause) {
	String message = getErrorTemplate(serialized_data, "Unexpected deserialization failure", cause);
    return new RJSerializationError(serialized_data, message, cause);
  }
  
  private String getErrorTemplate(byte[] serialized_data, String message, Throwable cause) {
    String separator = System.getProperty("line.separator");
    StringBuilder builder = new StringBuilder();
    builder.append(separator + "---- Debugging information ----" + separator);
    builder.append("message             : " + message + separator);
    builder.append("cause-exception     : " + cause.getClass().getName() + separator);
    builder.append("cause-message       : " + cause.getLocalizedMessage() + separator);
    builder.append("DATA                : " + separator);
    builder.append(new String(serialized_data) + separator);
    builder.append("-------------------------------");
    return builder.toString();
  }
}
