
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.times
import com.nhaarman.mockitokotlin2.verify
import org.junit.jupiter.api.Test

class CloseableObjectsUsageTest {
    private val obj = Any()

    @Test
    fun `will call close on deregister of last user`() {
        val mockCloseable = mock<AutoCloseable>()
        CloseableObjectsUsage.register(obj, mockCloseable)
        CloseableObjectsUsage.deregister(obj)
        verify(mockCloseable, times(1)).close()
    }

    @Test
    fun `will not call close on deregister if there are remaining active users`() {
        val mockCloseable = mock<AutoCloseable>()
        CloseableObjectsUsage.register(obj, mockCloseable)
        val obj2 = Any()
        CloseableObjectsUsage.register(obj2, mockCloseable)
        CloseableObjectsUsage.deregister(obj)
        verify(mockCloseable, times(0)).close()
        CloseableObjectsUsage.deregister(obj2)
        verify(mockCloseable, times(1)).close()
    }
}