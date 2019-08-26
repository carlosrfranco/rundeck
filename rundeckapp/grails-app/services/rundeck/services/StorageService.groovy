/*
 * Copyright 2016 SimplifyOps, Inc. (http://simplifyops.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package rundeck.services

import com.dtolabs.rundeck.core.authorization.AuthContext
import com.dtolabs.rundeck.core.storage.AuthStorageTree
import com.dtolabs.rundeck.core.storage.StorageUtil
import com.dtolabs.rundeck.core.storage.keys.KeyStorageTree
import com.dtolabs.rundeck.core.storage.keys.KeyStorageUtil
import org.rundeck.storage.api.PathUtil
import org.rundeck.storage.data.DataUtil

/**
 * Service layer access to the authorized storage
 */
class StorageService {
    AuthStorageTree authRundeckStorageTree

    def hasPath(AuthContext context, String path) {
        getServiceTree().hasPath(context, PathUtil.asPath(path))
    }

    def hasResource(AuthContext context, String path) {
        getServiceTree().hasResource(context, PathUtil.asPath(path))
    }

    def getResource(AuthContext context, String path) {
        getServiceTree().getPath(context, PathUtil.asPath(path))
    }

    def updateResource(AuthContext context, String path, Map<String, String> meta, InputStream data) {
        getServiceTree().updateResource(context, PathUtil.asPath(path), DataUtil.withStream(data, meta,
                StorageUtil.factory()))
    }
    def createResource(AuthContext context, String path, Map<String, String> meta, InputStream data) {
        getServiceTree().createResource(context, PathUtil.asPath(path), DataUtil.withStream(data, meta, StorageUtil.factory()))
    }

    def listDir(AuthContext context, String path) {
        getServiceTree().listDirectory(context, PathUtil.asPath(path))
    }

    def delResource(AuthContext context, String path) {
        getServiceTree().deleteResource(context, PathUtil.asPath(path))
    }

    protected AuthStorageTree getServiceTree() {
        return authRundeckStorageTree
    }
    /**
     * Return a tree using the authorization context
     * @param ctx auth context
     * @return StorageTree
     */
    def KeyStorageTree storageTreeWithContext(AuthContext ctx) {
        KeyStorageUtil.keyStorageWrapper StorageUtil.resolvedTree(ctx, authRundeckStorageTree)
    }
}
