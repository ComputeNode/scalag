package com.scalag.vulkan.compute;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * @author MarconZet
 * Created 25.04.2020
 */
@RequiredArgsConstructor
@Getter
public class LayoutInfo {
    private final int
            set,
            binding,
            size;
}
