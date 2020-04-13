package com.unihogsoft.scalag.vulkan.core;

import com.unihogsoft.scalag.vulkan.utility.VulkanAssertionError;
import com.unihogsoft.scalag.vulkan.utility.VulkanObject;
import org.lwjgl.BufferUtils;
import org.lwjgl.PointerBuffer;
import org.lwjgl.system.MemoryUtil;
import org.lwjgl.vulkan.VkApplicationInfo;
import org.lwjgl.vulkan.VkInstance;
import org.lwjgl.vulkan.VkInstanceCreateInfo;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

import static org.lwjgl.system.MemoryUtil.*;
import static org.lwjgl.system.MemoryUtil.memFree;
import static org.lwjgl.vulkan.EXTDebugReport.VK_EXT_DEBUG_REPORT_EXTENSION_NAME;
import static org.lwjgl.vulkan.VK10.*;
import static org.lwjgl.vulkan.VK10.VK_SUCCESS;

/**
 * @author MarconZet
 * Created 13.04.2020
 */
public class Instance extends VulkanObject {
    private static final String[] VALIDATION_LAYERS_INSTANCE_EXTENSIONS = {VK_EXT_DEBUG_REPORT_EXTENSION_NAME};
    private static final String[] INSTANCE_EXTENSIONS = {};
    private static final String[] VALIDATION_LAYERS = {"VK_LAYER_KHRONOS_validation"};
    private final boolean enableValidationLayers;

    private VkInstance instance;

    public Instance() {
        this.enableValidationLayers = System.getProperty("debug") != null;
        create();
    }

    @Override
    protected void init() {
        VkApplicationInfo appInfo = VkApplicationInfo.calloc()
                .sType(VK_STRUCTURE_TYPE_APPLICATION_INFO)
                .pNext(NULL)
                .pApplicationName(memUTF8("ScalaG MVP"))
                .pEngineName(memUTF8("ScalaG Computing Engine"))
                .applicationVersion(VK_MAKE_VERSION(0, 1, 0))
                .engineVersion(VK_MAKE_VERSION(0, 1, 0))
                .apiVersion(VK_API_VERSION_1_0);

        PointerBuffer ppEnabledExtensionNames = getInstanceExtensions();
        PointerBuffer ppEnabledLayerNames = getValidationLayers();

        VkInstanceCreateInfo pCreateInfo = VkInstanceCreateInfo.calloc()
                .sType(VK_STRUCTURE_TYPE_INSTANCE_CREATE_INFO)
                .pNext(NULL)
                .pApplicationInfo(appInfo)
                .ppEnabledExtensionNames(ppEnabledExtensionNames)
                .ppEnabledLayerNames(ppEnabledLayerNames);
        PointerBuffer pInstance = BufferUtils.createPointerBuffer(1);
        int err = vkCreateInstance(pCreateInfo, null, pInstance);
        long instance = pInstance.get(0);
        if (err != VK_SUCCESS) {
            throw new VulkanAssertionError("Failed to create VkInstance", err);
        }
        this.instance = new VkInstance(instance, pCreateInfo);

        memFree(appInfo.pApplicationName());
        memFree(appInfo.pEngineName());
        memFree(ppEnabledExtensionNames);
        memFree(ppEnabledLayerNames);
        appInfo.free();
        pCreateInfo.free();
    }

    private PointerBuffer getValidationLayers() {
        if (enableValidationLayers) {
            PointerBuffer validationLayers = memAllocPointer(VALIDATION_LAYERS.length);
            for (String layer : VALIDATION_LAYERS) {
                validationLayers.put(memASCII(layer));
            }
            return validationLayers.flip();
        } else {
            return null;
        }
    }

    private PointerBuffer getInstanceExtensions() {
        List<String> extensions = Arrays.asList(INSTANCE_EXTENSIONS);
        if (enableValidationLayers)
            extensions.addAll(Arrays.asList(VALIDATION_LAYERS_INSTANCE_EXTENSIONS));

        PointerBuffer ppEnabledExtensionNames = memAllocPointer(extensions.size());
        extensions.forEach(x -> ppEnabledExtensionNames.put(memASCII(x)));
        return ppEnabledExtensionNames.flip();
    }

    @Override
    protected void close() {
        vkDestroyInstance(instance, null);
    }

    public VkInstance get() {
        return instance;
    }
}
