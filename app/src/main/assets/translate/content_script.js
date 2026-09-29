let port = browser.runtime.connectNative("browser");
let nodes = [];

port.onMessage.addListener(response => {
    if(response.type == "translate") {
        let model_id = response.model_id;
        let message = {};

        var treeWalker = document.createTreeWalker(
          document.body,
          NodeFilter.SHOW_TEXT,
          {
            acceptNode: function(node) {
              if (node.nextElementSibling != null ||
                  (node.textContent.length <= 2 && node.textContent.search("\n") != -1)) {
                return NodeFilter.FILTER_SKIP;
              }
              else {
                return NodeFilter.FILTER_ACCEPT;
              }
            }
          },
          false
        );

        let count = 0;
        message.result = {};
        while(treeWalker.nextNode()) {
            nodes.push(treeWalker.currentNode);
            message.result[count] = treeWalker.currentNode.nodeValue;
            count += 1;
        }

        message.model_id = model_id;

        port.postMessage(message);
    } else if(response.type == "result") {
        nodes[response.id].nodeValue = response.translation;
    }
});