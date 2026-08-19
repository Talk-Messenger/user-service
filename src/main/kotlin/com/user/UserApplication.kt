package com.user

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.scheduling.annotation.EnableScheduling

@EnableScheduling
@SpringBootApplication
class UserApplication

fun main(args: Array<String>) {
	runApplication<UserApplication>(*args)
}
