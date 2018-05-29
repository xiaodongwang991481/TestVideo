package com.example.xiaodong.testvideo

import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.Button
import android.widget.TextView

class CameraDestPropertyAdapter(
        val context: CameraDestEditActivity, val cameraDestProperties: ArrayList<CameraDestProperty>
) : BaseAdapter() {

    inner class DeleteCameraDestProperty(val cameraDestProperty: CameraDestProperty) : View.OnClickListener {
        override fun onClick(v: View?) {
            context.onButtonClickDelete(cameraDestProperty)
        }
    }

    override fun getCount(): Int {
        return cameraDestProperties.size
    }

    override fun getItem(position: Int): Any {
        return cameraDestProperties.get(position)
    }

    override fun getItemId(position: Int): Long {
        return position.toLong()
    }

    override fun getView(position: Int, convertView: View?, parent: ViewGroup) : View {
        val view: View = convertView ?: LayoutInflater.from(context).
                inflate(R.layout.camera_dest_property_layout, parent, false)
        Log.i(LOG_TAG, "getView at $position")
        val currentItem: CameraDestProperty = getItem(position) as CameraDestProperty
        val itemName: TextView = view.findViewById(R.id.camera_dest_property_name)!!
        itemName.setText(currentItem.name)
        val itemValue: TextView = view.findViewById(R.id.camera_dest_property_value) as TextView
        itemValue.setText(currentItem.value)
        val deleteItem: Button = view.findViewById(R.id.delete_camera_dest_property) as Button
        deleteItem.setOnClickListener(DeleteCameraDestProperty(currentItem))
        return view
    }

    companion object {
        private val LOG_TAG = "CameraDestPropertyAdapter"
    }
}