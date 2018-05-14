package com.example.xiaodong.testvideo

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseExpandableListAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.TextView

class CameraDestAdapter(val context: Context, val cameraDests: ArrayList<CameraDest>) : BaseExpandableListAdapter() {
    val app: CameraEditActivity? = context as? CameraEditActivity

    inner class DeleteCameraDest(val cameraDest: CameraDest) : View.OnClickListener {
        override fun onClick(v: View?) {
            v?.let {
                app?.let {
                    app.onButtonClickDelete(cameraDest)
                }
            }
        }
    }

    inner class EditCameraDest(val cameraDest: CameraDest) : View.OnLongClickListener {
        override fun onLongClick(v: View?): Boolean {
            v?.let {
                app?.let {
                    app.onButtonClickEdit(cameraDest)
                }
            }
            return true
        }
    }

    inner class DeleteCameraDestProperty(
            val cameraDest: CameraDest, val cameraDestProperty: CameraDestProperty
    ) : View.OnClickListener {
        override fun onClick(v: View?) {
            v?.let {
                app?.let {
                    app.onButtonClickDeleteProperty(cameraDest, cameraDestProperty)
                }
            }
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
            isLastChild: Boolean, convertView: View?, parent: ViewGroup?): View? {
        val parentItem: CameraDest? =  getGroup(groupPosition) as? CameraDest
        val currentItem: CameraDestProperty? = getChild(groupPosition, childPosition) as? CameraDestProperty
        val view: View? = convertView ?: LayoutInflater.from(context).
                inflate(R.layout.camera_dest_property_layout, parent, false)
        val itemName: TextView? = view?.findViewById(R.id.camera_dest_property_name)
        itemName?.let {
            currentItem?.let {
                itemName.setText(currentItem.name)
            }
        }
        val itemValue: TextView? = view?.findViewById(R.id.camera_dest_property_value)
        itemValue?.let {
            currentItem?.let {
                itemValue.setText(currentItem.value)
            }
        }
        val deleteItem: Button? = view?.findViewById(R.id.delete_camera_dest_property)
        deleteItem?.let {
            parentItem?.let {
                currentItem?.let {
                    deleteItem.setOnClickListener(DeleteCameraDestProperty(parentItem, currentItem))
                }
            }
        }
        return view
    }

    override fun getChildrenCount(groupPosition: Int): Int {
        return cameraDests.get(groupPosition).dest_properties.size
    }

    override fun getGroup(groupPosition: Int): Any {
        return cameraDests.get(groupPosition)
    }

    override fun getGroupCount(): Int {
        return cameraDests.size
    }

    override fun getGroupId(groupPosition: Int): Long {
        return groupPosition.toLong()
    }

    override fun getGroupView(
            groupPosition: Int, isExpanded: Boolean, convertView: View?, parent: ViewGroup
    ): View? {
        val currentItem: CameraDest? = getGroup(groupPosition) as? CameraDest
        val view: View? = convertView ?: LayoutInflater.from(context).
                inflate(R.layout.camera_dest_layout, parent, false)
        val itemName: TextView? = view?.findViewById(R.id.camera_dest)
        itemName?.let {
            currentItem?.let {
                itemName.setText(currentItem.name)
            }
        }
        val deleteItem: Button? = view?.findViewById(R.id.delete_camera_dest)
        deleteItem?.let {
            currentItem?.let {
                deleteItem.setOnClickListener(DeleteCameraDest(currentItem))
            }
        }
        view?.let {
            currentItem?.let {
                view?.setOnLongClickListener(EditCameraDest(currentItem))
            }
        }
        return view
    }


}