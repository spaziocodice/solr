/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.solr.filestore;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.file.Path;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Predicate;
import org.apache.solr.filestore.FileStoreAPI.MetaData;
import org.apache.zookeeper.server.ByteBufferInputStream;

/** The interface to be implemented by any package store provider * @lucene.experimental */
public interface FileStore {

  /**
   * Store a file into the filestore. This should ensure that it is replicated across all nodes in
   * the cluster
   */
  void put(FileEntry fileEntry) throws IOException;

  /** read file content from a given path */
  void get(String path, Consumer<FileEntry> filecontent, boolean getMissing) throws IOException;

  /** Fetch a resource from another node internal API */
  boolean fetch(String path, String from);

  List<FileDetails> list(String path, Predicate<String> predicate);

  /** Sync a local file to all nodes. All the nodes are asked to pull the file from this node */
  void syncToAllNodes(String path) throws IOException;

  /** get the real path on filesystem */
  Path getRealPath(String path);

  /** The type of the resource */
  FileType getType(String path, boolean fetchMissing);

  /** Get all the keys in the package store. The data is a .DER file content */
  Map<String, byte[]> getKeys() throws IOException;

  /**
   * Refresh the files in a path. May be this node does not have all files
   *
   * @param path the path to be refreshed.
   */
  void refresh(String path);

  /** Delete a file cluster-wide */
  void delete(String path);

  /** Delete file from local file system */
  void deleteLocal(String path);

  public class FileEntry {
    final ByteBuffer buf;
    final MetaData meta;
    final String path;

    FileEntry(ByteBuffer buf, MetaData meta, String path) {
      this.buf = buf;
      this.meta = meta;
      this.path = path;
    }

    public String getPath() {
      return path;
    }

    public InputStream getInputStream() {
      if (buf != null) return new ByteBufferInputStream(buf);
      return null;
    }

    /** For very large files , only a stream would be available This method would return null; */
    public ByteBuffer getBuffer() {
      return buf;
    }

    public MetaData getMetaData() {
      return meta;
    }
  }

  enum FileType {
    FILE,
    DIRECTORY,
    NOFILE,
    METADATA
  }

  interface FileDetails {

    String getSimpleName();

    MetaData getMetaData();

    Date getTimeStamp();

    long size();

    boolean isDir();
  }
}
