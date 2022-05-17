import logging
import threading
import json
import websocket
import ssl
import multiprocessing as mp
from datetime import datetime

import tornado.web
import tornado.websocket
import tornado.ioloop

from perspective import Table, PerspectiveTornadoHandler, PerspectiveManager

logging.basicConfig(level=logging.INFO, format="%(asctime)s - %(process)d %(levelname)s: %(message)s")


class WebTickerPlant(object):

    def __init__(self, symbol, data_queue):
        """A datasource that interfaces with our Websockets API to
        receive live order book data and submits it to a queue in order
        to update the Perspective table."""
        self.symbol = symbol
        self.data_queue = data_queue
        self.url = "ws://192.168.0.201:8081"

    def start(self):
        """Make the API connection."""
        logging.info("Connecting to {} order book".format(self.symbol))
        self.ws = websocket.WebSocketApp(self.url, on_message=self.on_message)
        self.ws.run_forever(sslopt={"cert_reqs": ssl.CERT_NONE})

    def update_table(self, msg):
        """Given a report message from TickerPlant, update the Perspective table."""
        updated_orderbook = [{
            "symbol": msg["symbol"],
            "side": msg["side"],
            "price": float(msg["executionPrice"]),
            "remaining": msg["newSize"],
            "timestamp": str(msg["timestamp"])
        }]

        
        updated_market = []
        if msg["executionQuantity"]:
            updated_market = [{
                "symbol": msg["symbol"],
                "side": msg["side"],
                "price": float(msg["executionPrice"]),
                "time": datetime.now()
            }]

        return updated_orderbook, updated_market

    def on_message(self, ws, msg):
        if msg is None:
            logging.warn("connection closed for symbol {}".format(self.symbol))
            return

        msg = json.loads(msg)
        self.data_queue.put(self.update_table(msg))


MANAGER = PerspectiveManager()
PSP_LOOP = tornado.ioloop.IOLoop()

ORDER_BOOK = Table({
    "symbol": str,
    "side": str,
    "price": float,
    "remaining": float,
    "timestamp": str
}, index="price")

MARKET = Table({"symbol": str, "price": float, "side": str, "time": datetime}, limit=10000,)


def perspective_thread():
    """Run Perspective on a separate thread using a Tornado IOLoop,
    which improves concurrent performance with multiple clients."""
    MANAGER.set_loop_callback(PSP_LOOP.add_callback)
    MANAGER.host_table("order_book", ORDER_BOOK)
    MANAGER.host_table("market", MARKET)
    PSP_LOOP.start()


def fetch_data(orderbook_table, market_table, data_queue):
    """Wait for the datasource to add new data to the queue, and call
    table.update() using the IOLoop's add_callback method in order to call
    the operation on the Perspective thread."""
    while True:
        new_orderbook, new_market = data_queue.get()
        PSP_LOOP.add_callback(orderbook_table.update, new_orderbook)
        if new_market:
            PSP_LOOP.add_callback(market_table.update, new_market)


def start():
    """Set up the server - we use a queue to manage the flow of data from the
    datasource to the Table. There are two processes: the main process which
    runs the Tornado server and two sub-threads, one thread for Perspective
    and another thread to fetch data from the queue, and the subprocess which
    runs the datasource and submits data to the queue in order to transfer it
    between processes."""
    orders_queue = mp.Queue()

    # The thread that fetches data from the queue and calls table.update
    order_fetcher_thread = threading.Thread(target=fetch_data, args=(ORDER_BOOK, MARKET, orders_queue))
    order_fetcher_thread.daemon = True
    order_fetcher_thread.start()

    # The thread that runs Perspective
    psp_thread = threading.Thread(target=perspective_thread)
    psp_thread.daemon = True
    psp_thread.start()

    # The process that runs the datasource
    orders = WebTickerPlant("NVDA", orders_queue)
    orders_process = mp.Process(target=orders.start)
    orders_process.start()

    app = tornado.web.Application([
        (
            r"/websocket",
            PerspectiveTornadoHandler,
            {"manager": MANAGER, "check_origin": True},
        ),
    ])

    # Tornado listens on the main process
    app.listen(8082, '0.0.0.0')
    app_loop = tornado.ioloop.IOLoop()
    app_loop.make_current()
    app_loop.start()


if __name__ == "__main__":
    start()