package com.example.xiaodong.testvideo

import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.Button
import android.widget.TextView

class CameraSourcePropertyAdapter(
        val context: CameraEditActivity, val cameraSourceProperties: ArrayList<CameraSourceProperty>
) : BaseAdapter() {

    inner class DeleteCameraSourceProperty(val cameraSourceProperty: CameraSourceProperty) : View.OnClickListener {
        override fun onClick(v: View?) {
            context.onButtonClickDeleteSourceProperty(cameraSourceProperty)
        }
    }

    override fun getCount(): Int {
        return cameraSourceProperties.size
    }

    override fun getItem(position: Int): Any {
        return cameraSourceProperties.get(position)
    }

    override fun getItemId(position: Int): Long {
        return position.toLong()
    }

    override fun getView(position: Int, convertView: View?, parent: ViewGroup) : View {
        val view: View = convertView ?: LayoutInflater.from(context).
                inflate(R.layout.camera_source_property_layout, parent, false)
        Log.i(LOG_TAG, "getView at $position")
        val currentItem: CameraSourceProperty = getItem(position) as CameraSourceProperty
        val itemName: TextView = view.findViewById(R.id.camera_source_property_name)!!
        itemName.setText(currentItem.name)
        val itemValue: TextView = view.findViewById(R.id.camera_source_property_value) as TextView
        itemValue.setText(currentItem.value)
        val deleteItem: Button = view.findViewById(R.id.delete_camera_source_property) as Button
        deleteItem.setOnClickListener(DeleteCameraSourceProperty(currentItem))
        return view
    }

    companion object {
        private val LOG_TAG = "CameraSourcePropertyAdapter"
    }
}