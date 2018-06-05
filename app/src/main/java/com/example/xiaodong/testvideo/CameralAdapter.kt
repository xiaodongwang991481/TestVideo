package com.example.xiaodong.testvideo

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.Button
import android.widget.TextView

class CameraAdapter(val context: MainActivity, val cameras: ArrayList<Camera>) : BaseAdapter() {

    inner class EditCamera(val camera: Camera) : View.OnLongClickListener {
        override fun onLongClick(v: View?): Boolean {
            context.onButtonClickEdit(camera)
            return true
        }
    }

    inner class DeleteCamera(val camera: Camera) : View.OnClickListener {
        override fun onClick(v: View?) {
            context.onButtonClickDelete(camera)

        }
    }

    override fun getCount() : Int {
        return cameras.size
    }

    override fun getItem(position: Int) : Any {
        return cameras.get(position)
    }

    override fun getItemId(position: Int) : Long {
        return position.toLong()
    }

    override fun getView(position: Int, convertView: View?, parent: ViewGroup) : View {
        val view: View = convertView ?: LayoutInflater.from(context).
                inflate(R.layout.cameras_layout, parent, false)
        val currentItem: Camera = getItem(position) as Camera
        val itemName: TextView = view.findViewById(R.id.camera)!!
        itemName.setText(currentItem.name)
        val deleteItem: Button = view.findViewById(R.id.delete_camera)!!
        deleteItem.setOnClickListener(DeleteCamera(currentItem))
        return view
    }
}