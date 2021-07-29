package clover.studio.sdk.call

import androidx.core.util.Supplier
import androidx.lifecycle.MutableLiveData

open class SupplierMutableLiveData<T>(supplier: Supplier<T>) : MutableLiveData<T>() {
    override fun getValue(): T {
        return super.getValue()!!
    }

    fun postValue(invoker: (T) -> Unit) {
        val value = value
        invoker.invoke(value)
        postValue(value)
    }

    init {
        value = supplier.get()
    }
}