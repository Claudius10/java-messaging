package com.example.messaging.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Builder(setterPrefix = "with")
@AllArgsConstructor
@Getter
@Setter
public class Message {

	private Long id;

	private String content;
}
