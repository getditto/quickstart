#include "tasks_util.h"

#include <iomanip>

std::string quote_dql_identifier(const std::string &identifier) {
  std::string result = "`";
  for (char c : identifier) {
    if (c == '`') {
      result += "``";
    } else {
      result += c;
    }
  }
  result += "`";
  return result;
}

std::string quote_dql_string_literal(const std::string &s) {
  std::string result = "'";
  for (char c : s) {
    if (c == '\'') {
      result += "''";
    } else {
      result += c;
    }
  }
  result += "'";
  return result;
}

std::ostream &hexdump(std::ostream &os, const void *data, std::size_t size,
                      bool include_ascii) {
  const auto *bdata = reinterpret_cast<const unsigned char *>(data);
  std::size_t offset = 0;
  std::string ascii;

  // Save the original stream state
  std::ios_base::fmtflags originalFlags = os.flags();
  char originalFill = os.fill();

  os << std::hex << std::noshowbase << std::right << std::setfill('0');

  for (size_t i = 0; i < size; ++i) {
    if (offset % 16 == 0) {
      if (offset != 0) {
        if (include_ascii) {
          os << "  |" << ascii << "|";
          ascii.clear();
        }
        os << "\n";
      }
      os << std::setw(8) << offset;
    }
    if (offset % 8 == 0) {
      os << " ";
    }
    auto ch =
        bdata[i]; // NOLINT(cppcoreguidelines-pro-bounds-pointer-arithmetic)
    os << " " << std::setw(2) << static_cast<unsigned>(ch);
    if (include_ascii) {
      ascii += (32 <= ch && ch <= 126) ? (char)ch : '.';
    }
    ++offset;
  }

  if (include_ascii) {
    while (offset % 16 != 0) {
      os << "   ";
      if (offset % 8 == 0) {
        os << " ";
      }
      ++offset;
    }
    os << "  |" << ascii << "|";
  }

  // Restore the original stream state
  os.flags(originalFlags);
  os.fill(originalFill);

  return os;
}
