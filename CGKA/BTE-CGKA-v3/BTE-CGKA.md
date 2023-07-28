### 消息格式
#### 1.Init Message
init()调用者构建Information发给服务器

Information中包含群组成员信息（成员ID、成员顺序、公钥、签名公钥）和发送者信息
以及加密的节点信息

服务器收到Information后，先存储所有用户信息（只是模拟用，实际当中不会这么做），
然后更新本地sTree，最后为除了发送者外的每个用户构建MyMessage并发送。
MyMessage中应该包括包含群组成员信息（成员ID、成员顺序、公钥、签名公钥）和发送者信息，
和Information中没什么区别，加密信息要用对应数据
appendMsg为空

#### 2.Update Message
update()调用者构建Information发给服务器
Information中不包含群组成员信息，只包含发送者信息和加密的节点信息
服务器更新本地stree，为除了发送者外的每个用户构建MyMessage并发送。
MyMessage包含发送者信息和加密的节点信息

更新后不在自己子树上的结点也可以设为非空，但无法拿到种子，只能拿到它的公钥
所以在发送消息时，除了发送加密结点，还要发送路径上的所有公钥