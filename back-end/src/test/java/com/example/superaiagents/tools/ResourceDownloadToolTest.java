package com.example.superaiagents.tools;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;

class ResourceDownloadToolTest {

    @Test
    void downloadResource() {
        ResourceDownloadTool tool = new ResourceDownloadTool();
        String url = "https://picsum.photos/200/300"; // 替换为新的图片地址
        String fileName = "test-image.jpg";
        String result = tool.downloadResource(url, fileName);
        assertNotNull(result);
        System.out.println(result); // 查看下载结果
    }
}