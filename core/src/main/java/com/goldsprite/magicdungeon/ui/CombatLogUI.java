package com.goldsprite.magicdungeon.ui;

import com.badlogic.gdx.scenes.scene2d.actions.Actions;
import com.badlogic.gdx.scenes.scene2d.utils.Drawable;
import com.badlogic.gdx.utils.Align;
import com.kotcrab.vis.ui.widget.VisLabel;
import com.kotcrab.vis.ui.widget.VisTable;
import java.util.ArrayList;
import java.util.List;

public class CombatLogUI extends VisTable {
    private VisLabel msgLabel;
    private List<String> logMessages = new ArrayList<>();
    private static final int MAX_LOGS = 8;

    public CombatLogUI() {
        super();
        init();
    }
    
    public CombatLogUI(Drawable background) {
        super();
        setBackground(background);
        init();
    }

    private void init() {
        msgLabel = new VisLabel("欢迎来到地下城", "small");
        msgLabel.setFontScale(0.8f);
        msgLabel.setWrap(true);
        msgLabel.setAlignment(Align.topLeft);
        
        this.add(msgLabel).width(500).pad(10).top().left();
    }

    public void showMessage(String msg) {
        logMessages.add(msg);
        if (logMessages.size() > MAX_LOGS) {
            logMessages.remove(0);
        }
        StringBuilder sb = new StringBuilder();
        for (String s : logMessages) {
            sb.append(s).append("\n");
        }
        msgLabel.setText(sb.toString());
        
        // Auto fade out
        msgLabel.clearActions();
        msgLabel.getColor().a = 1;
        msgLabel.addAction(Actions.sequence(Actions.delay(3f), Actions.fadeOut(2f)));
    }
    
    public void clearLog() {
        logMessages.clear();
        msgLabel.setText("");
    }
}
