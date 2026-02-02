package com.goldsprite.magicdungeon.core.annotations;
import java.lang.annotation.*;
/** 强制隐藏字段 */
@Retention(RetentionPolicy.RUNTIME) @Target(ElementType.FIELD)
public @interface Hide {}
