package com.goldsprite.neonskel.utils;

import com.badlogic.gdx.graphics.g2d.TextureRegion;

/**
 * 资源提供者接口
 * 核心库只管要图片，不管图片从哪来（Atlas, InternalFile, 还是网络）
 */
public interface NeonAssetProvider {
    TextureRegion findRegion(String name);
}
