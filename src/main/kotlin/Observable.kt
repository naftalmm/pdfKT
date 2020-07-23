interface Observer {
    fun update(event: ObservableEvent)

    fun subscribeTo(vararg observables: Observable<*>) {
        observables.forEach { it.addObserver(this) }
    }
}

sealed class ObservableEvent
data class PageSelected(val pageIndex: Int) : ObservableEvent()
object ThumbnailLoaded : ObservableEvent()

interface Observable<T : ObservableEvent> {
    val subscribers: MutableList<Observer>

    fun addObserver(observer: Observer) {
        subscribers.add(observer)
    }

    fun notifySubscribers() = subscribers.forEach { it.update(getEvent()) }
    fun getEvent(): T
}

interface SelectedPageObservable : Observable<PageSelected> {
    fun getLatestSelectedPageIndex(): Int
    override fun getEvent() = PageSelected(getLatestSelectedPageIndex())
}
