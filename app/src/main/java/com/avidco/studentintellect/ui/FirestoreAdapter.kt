package com.avidco.studentintellect.ui

import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.firestore.*

abstract class FirestoreAdapter<VH : RecyclerView.ViewHolder>(private val query: Query) :
    RecyclerView.Adapter<VH>(), EventListener<QuerySnapshot> {

    private var registration: ListenerRegistration? = null
    private var snapshots = mutableListOf <DocumentSnapshot>()

    open fun startListening() {
        if (registration == null) {
            registration = query.addSnapshotListener(MetadataChanges.INCLUDE,this)
        }
    }

    open fun stopListening() {
        if (registration != null) {
            registration!!.remove()
            registration = null
        }

        snapshots.clear()
    }

    override fun onEvent(
        documentSnapshots: QuerySnapshot?,
        exception: FirebaseFirestoreException?
    ) {
        if (exception != null) {
            println(exception.message)
            return
        }

        for (change in documentSnapshots!!.documentChanges) {
            when (change.type) {
                DocumentChange.Type.ADDED -> onDocumentAdded(change)
                DocumentChange.Type.MODIFIED -> onDocumentModified(change)
                DocumentChange.Type.REMOVED -> onDocumentRemoved(change)
            }

            val source = if (documentSnapshots.metadata.isFromCache)
                "local cache"
            else
                "server"

            println("Data fetched from $source")
        }
    }

    protected open fun onDocumentAdded(change: DocumentChange) {
        snapshots.add(change.newIndex, change.document)
        notifyItemInserted(change.newIndex)
    }

    protected open fun onDocumentModified(change: DocumentChange) {
        if (change.oldIndex == change.newIndex) {
            snapshots[change.oldIndex] = change.document
            notifyItemChanged(change.oldIndex)
        } else {
            snapshots.removeAt(change.oldIndex)
            snapshots.add(change.newIndex, change.document)
            notifyItemMoved(change.oldIndex, change.newIndex)
        }
    }

    protected open fun onDocumentRemoved(change: DocumentChange) {
        snapshots.removeAt(change.oldIndex)
        notifyItemRemoved(change.oldIndex)
    }
    protected open fun onDocumentRemoved2(change: DocumentChange) {
        snapshots.clear()
        notifyItemRemoved(change.oldIndex)
    }
    protected open fun onDocumentRemoved(index: Int) {
        snapshots.removeAt(index)
        notifyItemRemoved(index)
    }

    override fun getItemCount(): Int {
        return snapshots.size
    }

    protected open fun getSnapshots() : MutableList<DocumentSnapshot> {
        return snapshots
    }

    protected open fun setSnapshots(documentSnapshots: MutableList<DocumentSnapshot>) {
        snapshots = documentSnapshots
    }

    protected open fun getSnapshot(index: Int): DocumentSnapshot {
        return snapshots[index]
    }
}