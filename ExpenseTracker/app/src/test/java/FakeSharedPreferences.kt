package com.example.expensetracker

import android.content.SharedPreferences

class FakeSharedPreferences : SharedPreferences {
    private val map = mutableMapOf<String, Any>()
    override fun getInt(key: String, def: Int) = map[key] as? Int?: def
    override fun getLong(key: String?, defValue: Long): Long {
        TODO("Not yet implemented")
    }

    override fun getString(key: String?, defValue: String?): String? {
        TODO("Not yet implemented")
    }

    override fun getStringSet(
        key: String?,
        defValues: Set<String?>?
    ): Set<String?>? {
        TODO("Not yet implemented")
    }

    override fun registerOnSharedPreferenceChangeListener(listener: SharedPreferences.OnSharedPreferenceChangeListener?) {
        TODO("Not yet implemented")
    }

    override fun unregisterOnSharedPreferenceChangeListener(listener: SharedPreferences.OnSharedPreferenceChangeListener?) {
        TODO("Not yet implemented")
    }

    override fun contains(key: String?): Boolean {
        TODO("Not yet implemented")
    }

    override fun edit() = FakeEditor(map)
    override fun getAll(): Map<String?, *>? {
        TODO("Not yet implemented")
    }

    override fun getBoolean(key: String?, defValue: Boolean): Boolean {
        TODO("Not yet implemented")
    }

    override fun getFloat(key: String?, defValue: Float): Float {
        TODO("Not yet implemented")
    }

    //... stub rest
    class FakeEditor(private val map: MutableMap<String, Any>) : SharedPreferences.Editor {
        override fun putInt(key: String, value: Int) = apply { map[key] = value }
        override fun putLong(
            key: String?,
            value: Long
        ): SharedPreferences.Editor? {
            TODO("Not yet implemented")
        }

        override fun putString(
            key: String?,
            value: String?
        ): SharedPreferences.Editor? {
            TODO("Not yet implemented")
        }

        override fun putStringSet(
            key: String?,
            values: Set<String?>?
        ): SharedPreferences.Editor? {
            TODO("Not yet implemented")
        }

        override fun remove(key: String?): SharedPreferences.Editor? {
            TODO("Not yet implemented")
        }

        override fun apply() {}
        override fun clear(): SharedPreferences.Editor? {
            TODO("Not yet implemented")
        }

        override fun commit(): Boolean {
            TODO("Not yet implemented")
        }

        override fun putBoolean(
            key: String?,
            value: Boolean
        ): SharedPreferences.Editor? {
            TODO("Not yet implemented")
        }

        override fun putFloat(
            key: String?,
            value: Float
        ): SharedPreferences.Editor? {
            TODO("Not yet implemented")
        }
        //... stub rest
    }
}