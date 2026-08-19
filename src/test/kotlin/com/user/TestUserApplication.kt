package com.user

import org.springframework.boot.fromApplication
import org.springframework.boot.with

fun main(args: Array<String>) {
    fromApplication<UserApplication>().with(TestcontainersConfiguration::class).run(*args)
}