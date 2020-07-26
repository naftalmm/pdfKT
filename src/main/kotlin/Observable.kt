import kotlin.reflect.KClass

interface Observer {
    fun update(event: ObservableEvent)

    fun subscribeTo(vararg multiObservables: Pair<MultiObservable, List<KClass<out ObservableEvent>>>) =
        multiObservables.forEach { (observable, events) ->
            events.forEach { observable.addObserver(it, this) }
        }

    fun subscribeTo(vararg observables: AbstractObservable) = observables.forEach {
        when (it) {
            is Observable<*> -> it.addObserver(this)
            is MultiObservable -> it.addAllEventsObserver(this)
        }
    }
}

sealed class ObservableEvent
data class PanelSelected(val panel: JPDFDocumentEditView.JSelectablePanel) : ObservableEvent()
object ThumbnailLoaded : ObservableEvent()
object AllPagesWereUnSelected : ObservableEvent()
object FirstPageWasSelected : ObservableEvent()

interface AbstractObservable
interface Observable<T : ObservableEvent> : AbstractObservable {
    val subscribers: MutableList<Observer>

    fun addObserver(observer: Observer) {
        subscribers.add(observer)
    }

    fun notifySubscribers() = subscribers.forEach { it.update(getEvent()) }
    fun getEvent(): T
}

interface MultiObservable : AbstractObservable {
    val subscribers: MutableMap<KClass<out ObservableEvent>, MutableList<Observer>>
    val allEventsSubscribers: MutableList<Observer>

    fun addObserver(event: KClass<out ObservableEvent>, observer: Observer) {
        subscribers.getOrPut(event) { ArrayList() }.add(observer)
    }

    fun addAllEventsObserver(observer: Observer) {
        allEventsSubscribers.add(observer)
    }

    fun notifySubscribers(event: ObservableEvent) {
        allEventsSubscribers.forEach { it.update(event) }
        subscribers[event::class]?.forEach { it.update(event) }
    }
}
