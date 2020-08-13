import java.lang.ref.WeakReference
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

    fun doNothing() = Unit
}

sealed class ObservableEvent
data class PanelSelected(val panel: JSelectablePanel) : ObservableEvent()
object ThumbnailLoaded : ObservableEvent()
object AllPagesWereUnSelected : ObservableEvent()
object FirstPageWasSelected : ObservableEvent()
data class PDFWasRemoved(val pdf: JPDFDocumentListItem) : ObservableEvent()
object AllPDFsWereRemoved : ObservableEvent()
object FirstPDFWasAdded : ObservableEvent()
object TitleImageChanged : ObservableEvent()
object AllPagesWereSelected: ObservableEvent()
object PenultPageWasSelected: ObservableEvent()

interface AbstractObservable
interface Observable<T : ObservableEvent> : AbstractObservable {
    val subscribers: MutableList<WeakReference<Observer>>

    fun addObserver(observer: Observer) {
        subscribers.add(WeakReference(observer))
    }

    fun notifySubscribers() = subscribers.forEach { it.get()?.update(getEvent()) }
    fun getEvent(): T
}

interface MultiObservable : AbstractObservable {
    val subscribers: MutableMap<KClass<out ObservableEvent>, MutableList<WeakReference<Observer>>>
    val allEventsSubscribers: MutableList<WeakReference<Observer>>

    fun addObserver(event: KClass<out ObservableEvent>, observer: Observer) {
        subscribers.getOrPut(event) { ArrayList() }.add(WeakReference(observer))
    }

    fun addAllEventsObserver(observer: Observer) {
        allEventsSubscribers.add(WeakReference(observer))
    }

    fun notifySubscribers(event: ObservableEvent) {
        allEventsSubscribers.forEach { it.get()?.update(event) }
        subscribers[event::class]?.forEach { it.get()?.update(event) }
    }
}
