// SPDX-License-Identifier: BSD-3-Clause

#include "base/process.h"

namespace mozc {

bool Process::OpenBrowser(absl::string_view) { return false; }

bool Process::SpawnMozcProcess(absl::string_view, absl::string_view,
                               size_t *pid) {
  if (pid) {
    *pid = 0;
  }
  return false;
}

}  // namespace mozc
