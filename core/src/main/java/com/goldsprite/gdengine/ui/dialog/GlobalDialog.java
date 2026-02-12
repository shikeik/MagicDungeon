package com.goldsprite.gdengine.ui.dialog;

import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.utils.Align;
import com.goldsprite.gdengine.ui.widget.BaseDialog;
import com.kotcrab.vis.ui.VisUI;
import com.kotcrab.vis.ui.widget.VisDialog;
import com.kotcrab.vis.ui.widget.VisLabel;
import com.kotcrab.vis.ui.widget.VisTable;
import com.kotcrab.vis.ui.widget.VisTextButton;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.scenes.scene2d.Actor;

/**
 * 全局通用弹窗
 * 驻留在 ToastStage 中，通过静态方法调用显示
 */
public class GlobalDialog extends BaseDialog {
    private static GlobalDialog instance;
    private VisLabel messageLabel;
    private Runnable onConfirm;

    private GlobalDialog() {
        super("提示");
        initUI();
        setVisible(false); // 默认隐藏，因为 GdxLauncher 会直接把它加到 Stage
    }

    public static GlobalDialog getInstance() {
        if (instance == null) {
            instance = new GlobalDialog();
        }
        return instance;
    }

    private void initUI() {
        // BaseDialog 已设置 modal=true 和 addCloseButton()
        setMovable(true);
        setResizable(false);
        
        // 内容区域
        messageLabel = new VisLabel("Message");
        messageLabel.setAlignment(Align.center);
        messageLabel.setWrap(true);
        getContentTable().add(messageLabel).width(400).pad(20).row();

        // 按钮区域
        VisTextButton okButton = new VisTextButton("确定");
        okButton.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                hide(); // VisDialog 内部方法
                if (onConfirm != null) {
                    onConfirm.run();
                }
            }
        });
        getButtonsTable().add(okButton).pad(10).bottom();

        pack();
    }

    public static void show(String title, String message) {
        show(title, message, null);
    }

    public static void show(String title, String message, Runnable onConfirm) {
        if (instance == null) getInstance(); // Ensure instance exists
        
        instance.getTitleLabel().setText(title);
        instance.messageLabel.setText(message);
        instance.onConfirm = onConfirm;
        
        instance.pack();
        
        // 如果已经有 Stage，直接显示
        if (instance.getStage() != null) {
             // 已经在 Stage 里了，直接确保可见和置顶
             instance.setVisible(true);
             instance.toFront();
             instance.centerWindow();
             instance.fadeIn(); // 使用 VisDialog 的动画效果
        } else {
             // 如果没有 Stage (理论上不应该，因为 GdxLauncher 早就加进去了)，这里只是防御性代码
             instance.setVisible(true);
        }
    }
}
