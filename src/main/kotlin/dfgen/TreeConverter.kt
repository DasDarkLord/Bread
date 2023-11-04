package dfgen

import dfk.template.DFTemplate
import parser.TreeNode

class TreeConverter {
    companion object {
        fun convertTree(tree: TreeNode, template: DFTemplate, objects: MutableMap<String, DFLObject> = mutableMapOf()): Any? {
            for (entry in astConverters) {
                if (entry.key == tree.type) return entry.value.convert(tree, template, objects)
            }

            throw UnsupportedOperationException("Unsupported tree type ${tree.type}")
        }
    }
}