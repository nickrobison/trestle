if (typeof window.URL.createObjectURL === 'undefined') {
  window.URL.createObjectURL = (): string => {
    // Do nothing
    // Mock this function for mapbox-gl to work
    return "";
  };
}

if (window.document) {
  window.document.createRange = () => ({
    setStart: () => {
    },
    setEnd: () => {
    },
    // @ts-ignore
    commonAncestorContainer: {
      nodeName: 'BODY',
      ownerDocument: document,
    },
    // @ts-ignore
    getBoundingClientRect: () => {

    }
  });
}


// @ts-ignore
class Worker {

  private readonly url;
  private readonly onmessage: () => void;
  constructor(stringUrl) {
    this.url = stringUrl;
    this.onmessage = () => {};
  }

  postMessage(_msg) {
    this.onmessage();
  }
}window.Worker = Worker;
