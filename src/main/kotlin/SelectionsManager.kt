class SelectionsManager : MultiObservable by MultiObservableImpl() {
    private val panelsOrder: LinkedHashMap<JSelectablePanel, Int> = LinkedHashMap()
    private var latestSelectedPanel: JSelectablePanel? = null
        set(value) {
            field = value
            notifySubscribers(PanelSelected(value ?: panelsOrder.asIterable().first().key))
        }

    val selectedPanels = LinkedHashSet<JSelectablePanel>()

    fun setPanelsOrder(panelsOrder: List<JSelectablePanel>, preserveSelection: Boolean = false) {
        val selectedPanelsIndexes =
            if (preserveSelection) selectedPanels.map { this.panelsOrder[it] ?: return } else emptyList()

        with(this.panelsOrder) {
            clear()
            panelsOrder.withIndex().forEach { this[it.value] = it.index }
        }

        selectedPanels.clear()
        if (!preserveSelection) {
            notifySubscribers(AllPagesWereUnSelected)
        } else {
            selectedPanelsIndexes.map { panelsOrder[it] }.forEach {
                it.select()
                selectedPanels.add(it)
            }
        }

        latestSelectedPanel = selectedPanels.lastOrNull()
    }

    fun toggleSelection(item: JSelectablePanel) {
        if (item.toggleSelect()) {
            selectedPanels.add(item)
        } else {
            selectedPanels.remove(item)
        }

        checkSelectionSizeAndNotifySubscribers()

        latestSelectedPanel = selectedPanels.lastOrNull()
    }

    private fun checkSelectionSizeAndNotifySubscribers() {
        when (selectedPanels.size) {
            0 -> notifySubscribers(AllPagesWereUnSelected)
            1 -> {
                notifySubscribers(FirstPageWasSelected)
                if (panelsOrder.size == 1) notifySubscribers(AllPagesWereSelected)
                else if (panelsOrder.size == 2) notifySubscribers(PenultPageWasSelected)
            }
            panelsOrder.size - 1 -> notifySubscribers(PenultPageWasSelected)
            panelsOrder.size -> notifySubscribers(AllPagesWereSelected)
        }
    }

    fun isAllPagesSelected() = selectedPanels.size == panelsOrder.size

    fun setSelection(item: JSelectablePanel) {
        selectedPanels.forEach { it.unselect() }
        selectedPanels.clear()
        toggleSelection(item)
    }

    fun rangeSelectFromLatestSelectedTo(item: JSelectablePanel) {
        val fromIndexInclusive = panelsOrder[latestSelectedPanel]
        if (fromIndexInclusive == null) {
            toggleSelection(item)
            return
        }

        val iterator = panelsOrder.keys.toList().listIterator(fromIndexInclusive)
        val reversed = fromIndexInclusive > panelsOrder[item]!!

        var it = latestSelectedPanel
        while (it != item) {
            it = if (reversed) iterator.previous() else iterator.next()
            it.select()
            selectedPanels.add(it)
            if (it == item) break
        }

        checkSelectionSizeAndNotifySubscribers()

        latestSelectedPanel = item
    }

    fun selectAll() {
        val wasNoSelectedPanels = selectedPanels.size == 0
        panelsOrder.keys.forEach { panel ->
            panel.select()
            selectedPanels.add(panel)
        }
        if (wasNoSelectedPanels) notifySubscribers(FirstPageWasSelected)
        notifySubscribers(AllPagesWereSelected)
    }

    fun selectAllFromLatestSelectedToFirst() {
        latestSelectedPanel ?: return
        rangeSelectFromLatestSelectedTo(panelsOrder.keys.asIterable().first())
    }

    fun selectAllFromLatestSelectedToLast() {
        latestSelectedPanel ?: return
        rangeSelectFromLatestSelectedTo(panelsOrder.keys.asIterable().last())
    }
}