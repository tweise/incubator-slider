/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.slider.core.registry.docstore;

import com.google.common.base.Charsets;
import com.google.common.base.Preconditions;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.hadoop.conf.Configuration;
import org.apache.slider.common.tools.ConfigHelper;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.StringWriter;
import java.util.Properties;

/**
 * Output a published configuration
 */
public abstract class PublishedConfigurationOutputter {

  protected final PublishedConfiguration owner;

  protected PublishedConfigurationOutputter(PublishedConfiguration owner) {
    this.owner = owner;
  }

  /**
   * Save the config to a destination file, in the format of this outputter
   * @param dest destination file
   * @throws IOException
   */
/* JDK7
  public void save(File dest) throws IOException {
    try(FileOutputStream out = new FileOutputStream(dest)) {
      save(out);
      out.close();
    }
  }
*/
  public void save(File dest) throws IOException {
    FileOutputStream out = null;
    try {
      out = new FileOutputStream(dest);
      save(out);
      out.close();
    } finally {
      org.apache.hadoop.io.IOUtils.closeStream(out);
    }
  }

  /**
   * Save the content. The default saves the asString() value
   * to the output stream
   * @param out output stream
   * @throws IOException
   */
  public void save(OutputStream out) throws IOException {
    IOUtils.write(asString(), out, Charsets.UTF_8);
  }
  /**
   * Convert to a string
   * @return the string form
   * @throws IOException
   */
  public abstract String asString() throws IOException;

  /**
   * Create an outputter for the chosen format
   * @param format format enumeration
   * @param owner owning config
   * @return the outputter
   */
  
  public static PublishedConfigurationOutputter createOutputter(ConfigFormat format,
      PublishedConfiguration owner) {
    Preconditions.checkNotNull(owner);
    switch (format) {
      case XML:
        return new XmlOutputter(owner);
      case PROPERTIES:
        return new PropertiesOutputter(owner);
      case JSON:
        return new JsonOutputter(owner);
      case ENV:
        return new EnvOutputter(owner);
      default:
        throw new RuntimeException("Unsupported format :" + format);
    }
  }
  
  public static class XmlOutputter extends PublishedConfigurationOutputter {


    private final Configuration configuration;

    public XmlOutputter(PublishedConfiguration owner) {
      super(owner);
      configuration = owner.asConfiguration();
    }

    @Override
    public void save(OutputStream out) throws IOException {
      configuration.writeXml(out);
    }

    @Override
    public String asString() throws IOException {
      return ConfigHelper.toXml(configuration);
    }

    public Configuration getConfiguration() {
      return configuration;
    }
  }
  
  public static class PropertiesOutputter extends PublishedConfigurationOutputter {

    private final Properties properties;

    public PropertiesOutputter(PublishedConfiguration owner) {
      super(owner);
      properties = owner.asProperties();
    }

    @Override
    public void save(OutputStream out) throws IOException {
      properties.store(out, "");
    }

    
    public String asString() throws IOException {
      StringWriter sw = new StringWriter();
      properties.store(sw, "");
      return sw.toString();
    }
  }
    
    
  public static class JsonOutputter extends PublishedConfigurationOutputter {

    public JsonOutputter(PublishedConfiguration owner) {
      super(owner);
    }

    @Override
    public void save(File dest) throws IOException {
        FileUtils.writeStringToFile(dest, asString(), Charsets.UTF_8);
    }

    @Override
    public String asString() throws IOException {
      return owner.asJson();
    }
  }


  public static class EnvOutputter extends PublishedConfigurationOutputter {

    public EnvOutputter(PublishedConfiguration owner) {
      super(owner);
    }

    @Override
    public void save(File dest) throws IOException {
      FileUtils.writeStringToFile(dest, asString(), Charsets.UTF_8);
    }

    @Override
    public String asString() throws IOException {
      if (!owner.entries.containsKey("content")) {
        throw new IOException("Configuration has no content field and cannot " +
            "be retrieved as type 'env'");
      }
      return owner.entries.get("content");
    }
  }


}
