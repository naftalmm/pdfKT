import java.lang.ref.WeakReference
import kotlin.reflect.KClass

interface Observer {
    fun update(event: ObservableEvent)

    fun subscribeTo(vararg multiObservables: Pair<MultiObservable, List<KClass<out ObservableEvent>>>) =
        multiObservables.forEach { (observable, events) ->
            events.forEach { observable.addObserver(it, this) }
        }

    fun subscribeTo(vararg observables: BaseObservable) = observables.forEach {
        when (it) {
            is Observable<*> -> it.addObserver(this)
            is MultiObservable -> it.addAllEventsObserver(this)
        }
    }
}

interface ObservableEvent
data class PanelSelected(val panel: JSelectablePanel) : ObservableEvent
object ThumbnailLoaded : ObservableEvent
object AllPagesWereUnSelected : ObservableEvent
object FirstPageWasSelected : ObservableEvent
data class PDFWasRemoved(val pdf: JPDFDocumentListItem) : ObservableEvent
object AllPDFsWereRemoved : ObservableEvent
object FirstPDFWasAdded : ObservableEvent
object TitleImageChanged : ObservableEvent
object AllPagesWereSelected : ObservableEvent
object PenultPageWasSelected : ObservableEvent

interface BaseObservable

interface Observable<T : ObservableEvent> : BaseObservable {
    fun addObserver(observer: Observer)
    fun notifySubscribers(event: T)
}

interface MultiObservable : BaseObservable {
    fun addObserver(event: KClass<out ObservableEvent>, observer: Observer)
    fun addAllEventsObserver(observer: Observer)
    fun notifySubscribers(event: ObservableEvent)
}

abstract class AbstractObservable<T : ObservableEvent> : Observable<T> {
    protected abstract val subscribers: MutableList<WeakReference<Observer>>

    override fun addObserver(observer: Observer) {
        subscribers.add(WeakReference(observer))
    }

    override fun notifySubscribers(event: T) = subscribers.forEach { it.get()?.update(event) }
}

abstract class AbstractMultiObservable : MultiObservable {
    protected abstract val subscribers: MutableMap<KClass<out ObservableEvent>, MutableList<WeakReference<Observer>>>
    protected abstract val allEventsSubscribers: MutableList<WeakReference<Observer>>

    override fun addObserver(event: KClass<out ObservableEvent>, observer: Observer) {
        subscribers.getOrPut(event) { ArrayList() }.add(WeakReference(observer))
    }

    override fun addAllEventsObserver(observer: Observer) {
        allEventsSubscribers.add(WeakReference(observer))
    }

    override fun notifySubscribers(event: ObservableEvent) {
        allEventsSubscribers.forEach { it.get()?.update(event) }
        subscribers[event::class]?.forEach { it.get()?.update(event) }
    }
}

class ObservableImpl<T : ObservableEvent> : AbstractObservable<T>() {
    override val subscribers = ArrayList<WeakReference<Observer>>()
}

class MultiObservableImpl : AbstractMultiObservable() {
    override val subscribers = hashMapOf<KClass<out ObservableEvent>, MutableList<WeakReference<Observer>>>()
    override val allEventsSubscribers = ArrayList<WeakReference<Observer>>()
}