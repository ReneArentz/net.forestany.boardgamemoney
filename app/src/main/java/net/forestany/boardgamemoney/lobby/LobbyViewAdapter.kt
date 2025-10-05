package net.forestany.boardgamemoney.lobby

import android.content.Context
import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import net.forestany.boardgamemoney.R

class LobbyViewAdapter(
    private val context: Context,
    private var list: List<String>,
    private val listClickListener: ListClickListener
) : RecyclerView.Adapter<LobbyViewAdapter.ViewHolder>() {
    companion object {
        private const val TAG = "LobbyViewAdapter"
    }

    interface ListClickListener {
        fun onListItemClicked(selectedItem: Int)
    }

    private var selectedElement: View? = null

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): LobbyViewAdapter.ViewHolder {
        val view = LayoutInflater.from(context).inflate(R.layout.list_item, parent, false)
        return ViewHolder(view)
    }

    override fun getItemCount(): Int {
        return list.size
    }

    override fun onBindViewHolder(holder: LobbyViewAdapter.ViewHolder, position: Int) {
        if (position < list.size) {
            holder.bind(position, list[position])
        } else {
            holder.bind()
        }
    }

    fun updateAllData(listUpdate: List<String>) {
        try {
            list = listUpdate
            notifyItemRangeChanged(0, if (list.isEmpty()) 1 else list.size)
        } catch (_: Exception) {

        }
    }

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val lL_main = itemView.findViewById<LinearLayout>(R.id.list_item)
        private val tV_listItemId = itemView.findViewById<TextView>(R.id.tV_listItemId)
        private val tV_listItemText = itemView.findViewById<TextView>(R.id.tV_listItemText)

        fun bind(id: Int, foo: String) {
            val s_foo: String = "#" + (id + 1)
            tV_listItemId.text = s_foo
            tV_listItemText.text = foo

            lL_main.setOnClickListener {
                selectedElement?.setBackgroundColor(Color.TRANSPARENT)

                if (lL_main != selectedElement) {
                    lL_main.setBackgroundColor(Color.LTGRAY)
                    selectedElement = lL_main
                    listClickListener.onListItemClicked(id)
                } else {
                    selectedElement = null
                    listClickListener.onListItemClicked(-1)
                }
            }
        }

        fun bind() {

        }
    }
}