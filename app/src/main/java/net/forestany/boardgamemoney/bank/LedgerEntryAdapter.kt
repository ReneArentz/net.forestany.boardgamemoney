package net.forestany.boardgamemoney.bank

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import net.forestany.boardgamemoney.R
import androidx.core.graphics.toColorInt
import net.forestany.boardgamemoney.GlobalInstance

class LedgerEntryAdapter(
    private val ledgerEntries: List<Message>
) : RecyclerView.Adapter<LedgerEntryAdapter.LedgerEntryAdapterViewHolder>() {
    class LedgerEntryAdapterViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        private val ledgerEntryIndex: TextView = view.findViewById(R.id.ledgerEntryIndex)
        private val ledgerEntryFromColor: View = view.findViewById(R.id.ledgerEntryFromColor)
        private val ledgerEntryFromName: TextView = view.findViewById(R.id.ledgerEntryFromName)
        private val ledgerEntryAmount: TextView = view.findViewById(R.id.ledgerEntryAmount)
        private val ledgerEntryToName: TextView = view.findViewById(R.id.ledgerEntryToName)
        private val ledgerEntryToColor: View = view.findViewById(R.id.ledgerEntryToColor)

        @SuppressLint("SetTextI18n")
        fun bind(ledgerEntry: Message) {
            ledgerEntryIndex.text = "#${ledgerEntry.tid}"
            ledgerEntryFromColor.setBackgroundColor(ledgerEntry.from.substring(ledgerEntry.from.indexOf("#")).toColorInt())
            ledgerEntryFromName.text = ledgerEntry.from.substring(0, ledgerEntry.from.indexOf("#"))
            ledgerEntryAmount.text = "${String.format(java.util.Locale.getDefault(), "%,d", ledgerEntry.amount)} ${GlobalInstance.get().getPreferences()["general_currency_text"]}"
            ledgerEntryToName.text = ledgerEntry.to.substring(0, ledgerEntry.to.indexOf("#"))
            ledgerEntryToColor.setBackgroundColor(ledgerEntry.to.substring(ledgerEntry.to.indexOf("#")).toColorInt())
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): LedgerEntryAdapterViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.bottom_sheet_dialog_ledger_item, parent, false)
        return LedgerEntryAdapterViewHolder(view)
    }

    override fun onBindViewHolder(holder: LedgerEntryAdapterViewHolder, position: Int) {
        holder.bind(ledgerEntries[position])
    }

    override fun getItemCount() = ledgerEntries.size
}