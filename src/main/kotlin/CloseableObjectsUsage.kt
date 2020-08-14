object CloseableObjectsUsage {
    private val objectToUsers: MutableMap<AutoCloseable, MutableSet<Any>> = hashMapOf()
    private val userToObject: MutableMap<Any, MutableSet<AutoCloseable>> = hashMapOf()

    fun register(user: Any, obj: AutoCloseable) {
        objectToUsers.getOrPut(obj) { HashSet() }.add(user)
        userToObject.getOrPut(user) { HashSet() }.add(obj)
    }

    fun deregister(user: Any) {
        val objects = userToObject.remove(user) ?: return
        for (obj in objects) {
            val users = objectToUsers[obj] ?: continue
            if (users.size == 1) {
                obj.close()
                objectToUsers.remove(obj)
            } else {
                users.remove(user)
            }
        }
    }
}