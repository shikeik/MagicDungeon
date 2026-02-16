# LibGDX UI 开发避坑指南

## 1. Tree.Node 是抽象类

### 问题描述
`com.badlogic.gdx.scenes.scene2d.ui.Tree.Node` 是一个抽象类 (`abstract`)，无法直接实例化。
如果在代码中直接使用 `new Tree.Node(...)` 或尝试将其当作具体类使用，会导致编译错误或 Raw Type 警告。
虽然 `VisTree` 提供了 `VisTree.Node`，但在处理泛型时，`Tree.Node` 本身的泛型定义 `<N extends Node, V, A extends Actor>` 较为复杂，容易引发 "Raw Use of Parameterized Class" 警告。

### 解决方案
必须继承 `Tree.Node` (或 `VisTree.Node`) 创建自定义的节点类。
这样可以：
1.  实例化具体的节点类。
2.  明确指定泛型类型，避免 Raw Type 警告。
3.  扩展节点功能（如存储自定义数据对象）。

### 示例代码
```java
// 错误做法
// Tree.Node node = new Tree.Node(actor); // 编译错误：Tree.Node 是抽象的

// 正确做法：定义具体实现类
public class MyNode extends Tree.Node<MyNode, String, Actor> {
    public MyNode(Actor actor) {
        super(actor);
    }
}

// 或者继承 VisTree.Node (如果是用 VisUI)
public class MyVisNode extends VisTree.Node {
    public MyVisNode(Actor actor) {
        super(actor);
    }
}
```

### 预防措施
- 凡是使用 `Tree` 组件，**必须**先定义一个继承自 `Tree.Node` 的静态内部类或独立类。
- **禁止**尝试直接实例化 `Tree.Node`。
- 在类定义中明确泛型参数，如 `class MyNode extends Tree.Node<MyNode, MyData, Actor>`。
