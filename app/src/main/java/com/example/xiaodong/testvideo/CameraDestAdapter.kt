package com.example.xiaodong.testvideo

import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseExpandableListAdapter
import android.widget.Button
import android.widget.TextView

class CameraDestAdapter(
        val context: CameraEditActivity, val cameraDests: ArrayList<CameraDest>
) : BaseExpandableListAdapter() {
    private val LOG_TAG = "CameraDestAdapter"

    inner class DeleteCameraDest(val cameraDest: CameraDest) : View.OnClickListener {
        override fun onClick(v: View?) {
            context.onButtonClickDelete(cameraDest)
        }
    }

    inner class EditCameraDest(val cameraDest: CameraDest) : View.OnLongClickListener {
        override fun onLongClick(v: View?): Boolean {
            context.onButtonClickEdit(cameraDest)
            return true
        }
    }

    inner class DeleteCameraDestProperty(
            val cameraDest: CameraDest, val cameraDestProperty: CameraDestProperty
    ) : View.OnClickListener {
        override fun onClick(v: View?) {
            context.onButtonClickDeleteProperty(cameraDest, cameraDestProperty)
        }
    }

    override fun hasStableIds(): Boolean {
        return true
    }

    override fun isChildSelectable(groupPosition: Int, childPosition: Int): Boolean {
        return true
    }

    override fun getChild(groupPosition: Int, childPosition: Int): Any {
        return cameraDests.get(groupPosition).dest_properties.get(childPosition)
    }

    override fun getChildId(groupPosition: Int, childPosition: Int): Long {
        return childPosition.toLong()
    }

    override fun getChildView(
            groupPosition: Int, childPosition: Int,
            isLastChild: Boolean, convertView: View?, parent: ViewGroup?): View {
        val parentItem: CameraDest =  getGroup(groupPosition) as CameraDest
        Log.i(LOG_TAG, "get group at $groupPosition: $parentItem")
        val currentItem: CameraDestProperty = getChild(groupPosition, childPosition) as CameraDestProperty
        Log.i(LOG_TAG, "get child at $groupPosition/$childPosition: $currentItem")
        val view: View = convertView ?: LayoutInflater.from(context).
                inflate(R.layout.camera_dest_property_layout, parent, false)
        val itemName: TextView = view.findViewById(R.id.camera_dest_property_name) as TextView
        itemName.setText(currentItem.name)
        val itemValue: TextView = view.findViewById(R.id.camera_dest_property_value) as TextView
        itemValue.setText(currentItem.value)
        val deleteItem: Button = view.findViewById(R.id.delete_camera_dest_property) as Button
        deleteItem.setOnClickListener(DeleteCameraDestProperty(parentItem, currentItem))
        var width = context.childIndicatorWidth
        view.setPadding(width, 0, 0, 0)
        return view
    }

    override fun getChildrenCount(groupPosition: Int): Int {
        val childSize = cameraDests.get(groupPosition).dest_properties.size
        Log.i(LOG_TAG, "child size at group $groupPosition: $childSize")
        return childSize
    }

    override fun getGroup(groupPosition: Int): Any {
        return cameraDests.get(groupPosition)
    }

    override fun getGroupCount(): Int {
        val groupSize = cameraDests.size
        Log.i(LOG_TAG, "group size: $groupSize")
        return groupSize
    }

    override fun getGroupId(groupPosition: Int): Long {
        return groupPosition.toLong()
    }

    override fun getGroupView(
            groupPosition: Int, isExpanded: Boolean, convertView: View?, parent: ViewGroup
    ): View {
        val currentItem: CameraDest = getGroup(groupPosition) as CameraDest
        Log.i(LOG_TAG, "get group $groupPosition: $currentItem")
        val view: View = convertView ?: LayoutInflater.from(context).
                inflate(R.layout.camera_dest_layout, parent, false)
        val itemName: TextView = view.findViewById(R.id.camera_dest) as TextView
        itemName.setText(currentItem.name)
        val deleteItem: Button = view.findViewById(R.id.delete_camera_dest) as Button
        deleteItem.setOnClickListener(DeleteCameraDest(currentItem))
        var width = context.groupIndicatorWidth
        view.setPadding(width, 0, 0, 0)
        return view
    }
}