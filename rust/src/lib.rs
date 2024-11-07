#![allow(clippy::new_without_default)]
#![warn(clippy::todo, clippy::panic, clippy::unwrap_used, clippy::expect_used)]

use std::sync::Arc;

pub mod config;
pub mod input;
pub mod tasks;
pub mod term;

/// Crate alias for a shutdown handle that carries an [`anyhow::Error`]
pub type Shutdown<T = Arc<anyhow::Error>> = async_shutdown::ShutdownManager<T>;

#[macro_export]
macro_rules! key {
    ($code:ident) => {
        $crate::input::Event::Key($crate::input::KeyEvent {
            code: $crate::input::KeyCode::$code,
            ..
        })
    };
    (Char($code:ident)) => {
        $crate::input::Event::Key($crate::input::KeyEvent {
            code: $crate::input::KeyCode::Char($code),
            ..
        })
    };
    (Char($code:literal)) => {
        $crate::input::Event::Key($crate::input::KeyEvent {
            code: $crate::input::KeyCode::Char($code),
            ..
        })
    };
    (Char(_)) => {
        $crate::input::Event::Key($crate::input::KeyEvent {
            code: $crate::input::KeyCode::Char(_),
            ..
        })
    };
}
