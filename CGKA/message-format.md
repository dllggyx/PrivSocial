
## Message Format
### 1. Init Message
The initiator of the `init()` call constructs an `Information` message to send to the server.

The `Information` contains group member information (member ID, member order, public key, signature public key), sender information, and encrypted node information.

Upon receiving the `Information`, the server first stores all user information (for simulation purposes only; this won't be done in practice), then updates the local sTree. Finally, it constructs a `MyMessage` for each user except the sender and sends it out. The `MyMessage` should include group member information (member ID, member order, public key, signature public key) and sender information, similar to the `Information` message, with encrypted information using the corresponding data. The `appendMsg` is empty.

### 2. Update Message
The initiator of the `update()` call constructs an `Information` message to send to the server. The Information does not contain group member information; it only includes the sender information and encrypted node information.

The server updates the local sTree and constructs a `MyMessage` for each user except the sender, then sends it out. The `MyMessage` includes sender information and encrypted node information.

Nodes not in the sender's subtree can be set as non-empty after the update, but they cannot obtain the seed, only the public key. Therefore, when sending a message, in addition to the encrypted nodes, all public keys on the path must also be sent.