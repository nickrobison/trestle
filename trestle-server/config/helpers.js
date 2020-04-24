/**
 * Created by nrobison on 1/17/17.
 */
const path = require("path");
const _root = path.resolve(__dirname, "..");
const git = require("nodegit");

/**
 * Builds path from root directory webpack is executed from
 * @param args
 * @returns {string}
 */
function root(args) {
    args = Array.prototype.slice.call(arguments, 0);
    return path.join.apply(path, [_root].concat(args));
}

/**
 * Get the current git branch branch
 * @returns {Promise<string>}
 */
async function getBranch() {
  const repo = await git.Repository.open("..");
  const branch = await repo.getCurrentBranch();

  return branch.shorthand();
}

exports.root = root;
exports.getBranch = getBranch;
