package com.dtolabs.rundeck.plugin

import com.dtolabs.rundeck.core.Constants
import com.dtolabs.rundeck.core.execution.workflow.steps.StepException
import com.dtolabs.rundeck.core.execution.workflow.steps.StepFailureReason
import com.dtolabs.rundeck.core.execution.workflow.steps.node.NodeStepException
import com.dtolabs.rundeck.core.plugins.Plugin
import com.dtolabs.rundeck.core.plugins.configuration.StringRenderingConstants
import com.dtolabs.rundeck.core.storage.StorageUtil
import com.dtolabs.rundeck.core.storage.files.FileStorageTree
import com.dtolabs.rundeck.plugins.ServiceNameConstants
import com.dtolabs.rundeck.plugins.descriptions.PluginDescription
import com.dtolabs.rundeck.plugins.descriptions.PluginProperty
import com.dtolabs.rundeck.plugins.descriptions.RenderingOption
import com.dtolabs.rundeck.plugins.descriptions.RenderingOptions
import com.dtolabs.rundeck.plugins.step.PluginStepContext
import com.dtolabs.rundeck.plugins.step.StepPlugin
import org.rundeck.storage.api.Path
import org.rundeck.storage.api.PathUtil
import org.rundeck.storage.data.DataUtil

@Plugin(service = ServiceNameConstants.WorkflowStep, name =SaveFilePlugin. TYPE)
@PluginDescription(title = "Save File",description = "Save a file on storage")
class SaveFilePlugin  implements StepPlugin {
    public static final String TYPE = "savefile"

    @PluginProperty(title = "Path to file",
            description = "Storage path to file",
            required = true)
    String path

    @PluginProperty(title = "file name",
            description = "A name to file",
            required = true)
    String name

    @PluginProperty(title = 'Overwrite if already exists', description = "Overwrite file if already exists")
    boolean overwrite = false

    @PluginProperty(title = "File URL",
            description = "URL to save a file from the filesystem")
    String directoryFile

    @PluginProperty(title = "File content",
            description = "File content")
    @RenderingOptions(
            [
                    @RenderingOption(key = StringRenderingConstants.GROUP_NAME, value = "Text file content"),
                    @RenderingOption(key = StringRenderingConstants.GROUPING, value = "secondary"),
                    @RenderingOption(key = StringRenderingConstants.DISPLAY_TYPE_KEY, value = "MULTI_LINE"),
                    @RenderingOption(key = StringRenderingConstants.CODE_SYNTAX_MODE, value = "text/x-markdown")
            ]
    )
    String content

    @Override
    void executeStep(PluginStepContext context, Map<String, Object> configuration) throws NodeStepException {
        Path filePath = PathUtil.asPath(path + "/" + name)
        FileStorageTree fileStorageTree = context.executionContext.getFileStorageTree()
        byte[] fileContent = fileContent()
        boolean hasFile = fileStorageTree.hasFile(filePath)

        if(!overwrite && hasFile){
            context.getLogger().log(Constants.ERR_LEVEL, "File already exists")
            throw new StepException(
                    "File already exists",
                    StepFailureReason.IOFailure
            )
        }

        Map<String, String> map = [:]
        InputStream stream = new ByteArrayInputStream(fileContent)

        if(hasFile && overwrite){
            fileStorageTree.updateResource(
                    filePath,
                    DataUtil.withStream(stream, map, StorageUtil.factory())
            )

            return
        }

        fileStorageTree.createResource(
                filePath,
                DataUtil.withStream(stream, map, StorageUtil.factory())
        )
    }

    private byte[] fileContent(){
        return directoryFile ? new File(directoryFile).getBytes() : content.getBytes()
    }
}
