public Flux<String> streamAnswer(String question, String userId) {
    List<String> tokens = List.of(
        "OmniBrain ", "is ", "an ", "enterprise ", "AI ", "platform."
    );

    return Flux.fromIterable(tokens)
            .delayElements(Duration.ofMillis(80));
}
