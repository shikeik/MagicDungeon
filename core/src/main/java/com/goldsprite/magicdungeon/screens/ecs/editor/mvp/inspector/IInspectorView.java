package com.goldsprite.magicdungeon.screens.ecs.editor.mvp.inspector;

public interface IInspectorView {
	void setPresenter(InspectorPresenter presenter);
	void rebuild(Object target); // GObject -> Object
	void updateValues();
	// void showAddComponentDialog(GObject target); // Deleted
}
