package com.example.xiaodong.testvideo

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseExpandableListAdapter

class CameraDestAdapter(val context: Context, val cameraDests: ArrayList<CameraDest>) : BaseExpandableListAdapter() {
    val app: CameraEditActivity? = context as? CameraEditActivity

    inner class AddCameraDestProperty(val cameraDest: CameraDest) : View.OnClickListener {
        override fun onClick(v: View?) {
            v?.let {
                app?.let {
                    this@CameraDestAdapter.onButtonClickAddProperty(cameraDest)
                }
            }
        }
    }

    inner class DeleteCameraDest()

    override fun getChild(groupPosition: Int, childPosition: Int): Any {
        return cameraDests.get(groupPosition).dest_properties.get(childPosition)
    }

    override fun getChildId(groupPosition: Int, childPosition: Int): Long {
        return childPosition
    }

    override fun getChildView(
            groupPosition: Int, childPosition: Int,
            isLastChild: Boolean, convertView: View?, parent: ViewGroup?): View? {
        val view: View? = convertView ?: LayoutInflater.from(context).
                inflate(R.layout.camera_dest_property_layout, parent, false)
        return view
    }

    override fun getChildrenCount(groupPosition: Int): Int {
        return cameraDests.get(groupPosition).dest_properties.size()
    }

    override fun getGroup(groupPosition: Int): Any {
        return cameraDests.get(groupPosition)
    }

    override fun getGroupCount(): Int {
        return cameraDests.size
    }

    override fun getGroupId(groupPosition: Int): Long {
        return groupPosition
    }

    override fun getGroupView(
            groupPosition: Int, isExpanded: Boolean, convertView: View?, parent: ViewGroup
    ): View? {
        val view: View? = convertView ?: LayoutInflater.from(context).
                inflate(R.layout.camera_dest_layout, parent, false)
        return view
    }

    fun onButtonClickAddProperty(val cameraDest: CameraDest) {
    }
}