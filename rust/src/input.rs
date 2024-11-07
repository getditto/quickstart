//! Input helpers
//!
//! Credit to [`hawkw`][0].
//! [0]: https://github.com/tokio-rs/console/blob/cbf6f56a16036ecf13548c4209fcc62f8a84bae2/tokio-console/src/term.rs

pub use crossterm::event::*;

pub fn should_quit(input: &Event) -> bool {
    use Event::*;
    use KeyCode::*;
    match input {
        Key(KeyEvent {
            code: Char('q'), ..
        }) => true,
        Key(KeyEvent {
            code: Char('c'),
            modifiers,
            ..
        })
        | Key(KeyEvent {
            code: Char('d'),
            modifiers,
            ..
        }) if modifiers.contains(KeyModifiers::CONTROL) => true,
        _ => false,
    }
}

pub fn is_space(input: &Event) -> bool {
    matches!(
        input,
        Event::Key(KeyEvent {
            code: KeyCode::Char(' '),
            ..
        })
    )
}
