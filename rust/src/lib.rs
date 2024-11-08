#![allow(clippy::new_without_default)]
#![warn(clippy::todo, clippy::panic, clippy::unwrap_used, clippy::expect_used)]

use std::sync::Arc;

use crossterm::event::{Event, KeyEvent, KeyModifiers};

pub mod tasks;
pub mod term;

/// Crate alias for a shutdown handle that carries an [`anyhow::Error`]
pub type Shutdown<T = Arc<anyhow::Error>> = async_shutdown::ShutdownManager<T>;

/// Helper macro for pattern matching on crossterm events
///
/// # Example
///
/// The two match arms here are equivalent:
///
/// ```
/// # use ditto_quickstart::key;
/// use crossterm::event::{Event, KeyCode, KeyEvent};
/// # fn example(event: Event) {
/// match event {
///     Event::Key(KeyEvent { code: KeyCode::Char(ch), .. }) => {
///         //
///     }
///     key!(Char(ch)) => {
///         //
///     }
///     _ => {}
/// }    
/// # }
/// ```
#[macro_export]
macro_rules! key {
    ($code:ident) => {
        crossterm::event::Event::Key(crossterm::event::KeyEvent {
            code: crossterm::event::KeyCode::$code,
            ..
        })
    };
    (Char($code:ident)) => {
        crossterm::event::Event::Key(crossterm::event::KeyEvent {
            code: crossterm::event::KeyCode::Char($code),
            ..
        })
    };
    (Char($code:literal)) => {
        crossterm::event::Event::Key(crossterm::event::KeyEvent {
            code: crossterm::event::KeyCode::Char($code),
            ..
        })
    };
    (Char(_)) => {
        crossterm::event::Event::Key(crossterm::event::KeyEvent {
            code: crossterm::event::KeyCode::Char(_),
            ..
        })
    };
}

pub fn should_quit(input: &Event) -> bool {
    use crossterm::event::{Event::*, KeyCode::*};
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
